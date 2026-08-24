package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.PaymentCallback
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.PaymentResult
import kotlinx.serialization.json.Json

class CieloResponseParser(private val json: Json) {

    fun parse(uri: Uri?): PaymentCallback {
        if (uri == null) return invalid()
        val response = runCatching { uri.getQueryParameter(PARAM_RESPONSE) }.getOrNull()
        return parse(response)
    }

    fun parse(responseParam: String?): PaymentCallback {
        if (responseParam.isNullOrBlank()) return invalid()

        val decoded = decodeBase64(responseParam) ?: return invalid()

        val errorEnvelope = decodeErrorOrNull(decoded)
        if (errorEnvelope?.code != null || !errorEnvelope?.reason.isNullOrBlank()) {
            return toFailure(errorEnvelope!!)
        }

        val order = decodeOrderOrNull(decoded) ?: return invalid()
        return toSuccess(order)
    }

    private fun toSuccess(order: CieloOrderDto): PaymentCallback {
        val reference = order.reference

        val payment = order.payments?.firstOrNull()
            ?: return PaymentCallback(reference, PaymentResult.Error(PaymentError.InvalidResponse))

        val pending = order.pendingAmount ?: 0L
        if (pending != 0L) {
            return PaymentCallback(reference, PaymentResult.Error(PaymentError.PartialPayment(pending)))
        }

        val result = when (payment.paymentFields?.statusCodeOrNull) {
            STATUS_PIX, STATUS_AUTHORIZED -> PaymentResult.Approved(payment.toPaymentInfo())

            STATUS_CANCELED -> PaymentResult.Canceled

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

        private const val CODE_USER_CANCELED = 1

        private const val STATUS_PIX = 0
        private const val STATUS_AUTHORIZED = 1
        private const val STATUS_CANCELED = 2
    }
}
