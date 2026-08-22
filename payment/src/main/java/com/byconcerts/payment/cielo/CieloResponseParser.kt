package com.byconcerts.payment.cielo

import android.util.Base64
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.PaymentResult
import kotlinx.serialization.json.Json

/**
 * Traduz o parâmetro `response` (BASE64 → JSON) do callback da Cielo para o
 * [PaymentResult] de domínio. Concentra TODO o conhecimento do formato Cielo —
 * é o único ponto do app que entende o payload bruto.
 */
class CieloResponseParser(private val json: Json) {

    fun parse(responseParam: String?): PaymentResult {
        if (responseParam.isNullOrBlank()) {
            return PaymentResult.Error(PaymentError.InvalidResponse)
        }

        val decoded = runCatching {
            String(Base64.decode(responseParam, Base64.DEFAULT))
        }.getOrElse { return PaymentResult.Error(PaymentError.InvalidResponse) }

        val dto = runCatching {
            json.decodeFromString<CieloResponseDto>(decoded)
        }.getOrElse { return PaymentResult.Error(PaymentError.InvalidResponse) }

        // Forma de erro/cancelamento: { code, reason }. code == 1 = cancelado.
        dto.code?.let { code ->
            return if (code == CODE_USER_CANCELED) {
                PaymentResult.Canceled
            } else {
                PaymentResult.Denied(reason = dto.reason ?: "Pagamento não concluído", code = code)
            }
        }

        val payment = dto.payments?.firstOrNull()
            ?: return PaymentResult.Error(PaymentError.InvalidResponse)

        // Pagamento parcial: se falta valor, NÃO é aprovado (erro de negócio).
        val pending = dto.pendingAmount ?: 0L
        if (pending != 0L) {
            return PaymentResult.Error(PaymentError.PartialPayment(pending))
        }

        return when (payment.paymentFields?.statusCode) {
            STATUS_PIX, STATUS_AUTHORIZED -> PaymentResult.Approved(
                PaymentInfo(
                    authCode = payment.authCode.orEmpty(),
                    cieloCode = payment.cieloCode.orEmpty(),
                    brand = payment.brand.orEmpty(),
                    maskedCard = payment.mask.orEmpty(),
                    amountInCents = payment.amount ?: 0L,
                ),
            )

            STATUS_CANCELED -> PaymentResult.Canceled

            else -> PaymentResult.Error(
                PaymentError.Unknown("statusCode=${payment.paymentFields?.statusCode}"),
            )
        }
    }

    private companion object {
        const val CODE_USER_CANCELED = 1
        const val STATUS_PIX = 0
        const val STATUS_AUTHORIZED = 1
        const val STATUS_CANCELED = 2
    }
}
