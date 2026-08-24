package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.PaymentCallback
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.PaymentResult
import kotlinx.serialization.json.Json

/**
 * Traduz o callback do deeplink da Cielo para o domínio. É o ÚNICO ponto do app
 * que conhece o formato bruto da Cielo.
 *
 * Contrato (sample oficial da Cielo + comportamento observado no emulador):
 *  - O retorno vem no query param `response`, em BASE64 — que pode chegar com
 *    quebras de linha embutidas na própria URI (o app da Cielo codifica com
 *    Base64.DEFAULT), daí a sanitização antes do decode.
 *  - Sucesso: a Order vem na RAIZ → `payments[]`, `pendingAmount`, `reference`.
 *  - Falha:   `{ "code": int, "reason": string, "order": {...} }`.
 *
 * IMPORTANTE — por que NÃO usamos `responsecode` como discriminador:
 * o sample oficial (Flutter) decide sucesso/falha pela presença do query param
 * `responsecode`. Verificado contra o emulador oficial da Cielo (v1.61.8), isso
 * é falso: o cancelamento também chega com `responsecode=0`. Confiar nesse
 * parâmetro fazia um CANCELAMENTO ser lido como sucesso e, por não haver
 * `payments[]`, virar InvalidResponse.
 *
 * O discriminador confiável é ESTRUTURAL: o envelope de falha tem `code`
 * (e/ou `reason`); a Order de sucesso não possui esses campos.
 *
 * O `reference` devolvido é a nossa chave de idempotência, e é o que amarra o
 * retorno à compra local.
 */
class CieloResponseParser(private val json: Json) {

    /** Faz o parse a partir da URI completa do callback. */
    fun parse(uri: Uri?): PaymentCallback {
        if (uri == null) return invalid()
        val response = runCatching { uri.getQueryParameter(PARAM_RESPONSE) }.getOrNull()
        return parse(response)
    }

    /** @param responseParam conteúdo do query param `response` (BASE64). */
    fun parse(responseParam: String?): PaymentCallback {
        if (responseParam.isNullOrBlank()) return invalid()

        val decoded = decodeBase64(responseParam) ?: return invalid()

        // Discriminação ESTRUTURAL (ver KDoc): `code`/`reason` ⇒ envelope de
        // falha; caso contrário, Order de sucesso.
        val errorEnvelope = decodeErrorOrNull(decoded)
        if (errorEnvelope?.code != null || !errorEnvelope?.reason.isNullOrBlank()) {
            return toFailure(errorEnvelope!!)
        }

        val order = decodeOrderOrNull(decoded) ?: return invalid()
        return toSuccess(order)
    }

    // ── Sucesso ──────────────────────────────────────────────────────────────

    private fun toSuccess(order: CieloOrderDto): PaymentCallback {
        val reference = order.reference

        val payment = order.payments?.firstOrNull()
            ?: return PaymentCallback(reference, PaymentResult.Error(PaymentError.InvalidResponse))

        // Pagamento parcial: se ainda falta valor, o pedido NÃO foi pago por
        // completo — tratamos como erro de negócio, nunca como aprovado.
        val pending = order.pendingAmount ?: 0L
        if (pending != 0L) {
            return PaymentCallback(reference, PaymentResult.Error(PaymentError.PartialPayment(pending)))
        }

        val result = when (payment.paymentFields?.statusCodeOrNull) {
            STATUS_PIX, STATUS_AUTHORIZED -> PaymentResult.Approved(payment.toPaymentInfo())

            STATUS_CANCELED -> PaymentResult.Canceled

            // Sem statusCode: se o pedido está quitado e há dados de autorização,
            // consideramos aprovado (o campo é opcional em alguns produtos).
            null -> if (order.isSettled && !payment.authCode.isNullOrBlank()) {
                PaymentResult.Approved(payment.toPaymentInfo())
            } else {
                PaymentResult.Error(PaymentError.Unknown("statusCode ausente"))
            }

            else -> PaymentResult.Error(
                PaymentError.Unknown("statusCode=${payment.paymentFields.statusCodeOrNull}"),
            )
        }
        return PaymentCallback(reference, result)
    }

    private val CieloOrderDto.isSettled: Boolean
        get() = (pendingAmount ?: 0L) == 0L && (paidAmount ?: 0L) > 0L

    private fun CieloPaymentDto.toPaymentInfo() = PaymentInfo(
        authCode = authCode.orEmpty(),
        cieloCode = cieloCode.orEmpty(),
        brand = brand.orEmpty(),
        maskedCard = mask.orEmpty(),
        amountInCents = amount ?: 0L,
        paymentId = id.orEmpty(),
        installments = installments ?: 0,
    )

    // ── Falha / cancelamento ─────────────────────────────────────────────────

    private fun toFailure(error: CieloErrorDto): PaymentCallback {
        val reference = error.order?.reference
        val result = if (error.code == CODE_USER_CANCELED) {
            PaymentResult.Canceled
        } else {
            PaymentResult.Denied(
                reason = error.reason?.takeIf { it.isNotBlank() } ?: "Pagamento não concluído",
                code = error.code,
            )
        }
        return PaymentCallback(reference, result)
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    /**
     * O sample oficial remove quebras de linha antes de decodificar (o Base64
     * pode chegar quebrado). Usamos também URL_SAFE-tolerante via DEFAULT.
     */
    private fun decodeBase64(raw: String): String? = runCatching {
        val sanitized = raw.replace("\n", "").replace("\r", "").trim()
        String(Base64.decode(sanitized, Base64.DEFAULT), Charsets.UTF_8)
    }.getOrNull()

    private fun decodeOrderOrNull(payload: String): CieloOrderDto? =
        runCatching { json.decodeFromString(CieloOrderDto.serializer(), payload) }.getOrNull()

    private fun decodeErrorOrNull(payload: String): CieloErrorDto? =
        runCatching { json.decodeFromString(CieloErrorDto.serializer(), payload) }.getOrNull()

    private fun invalid() =
        PaymentCallback(null, PaymentResult.Error(PaymentError.InvalidResponse))

    companion object {
        const val PARAM_RESPONSE = "response"
        const val PARAM_RESPONSE_CODE = "responsecode"

        /** code 1 = "CANCELADO PELO USUÁRIO". */
        private const val CODE_USER_CANCELED = 1

        // paymentFields.statusCode → 0 = PIX, 1 = autorizada, 2 = cancelamento.
        private const val STATUS_PIX = 0
        private const val STATUS_AUTHORIZED = 1
        private const val STATUS_CANCELED = 2
    }
}
