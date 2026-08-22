package com.byconcerts.payment.cielo

import android.util.Base64
import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Monta as URIs de deeplink da Cielo. Todo valor monetário trafega em CENTAVOS
 * (sem vírgula). O JSON é serializado e convertido para BASE64 (NO_WRAP).
 */
class CieloRequestCodec(
    private val json: Json,
    private val credentials: CieloCredentials,
) {
    /** URI de checkout: lio://payment?request=<base64>&urlCallback=order://response */
    fun buildCheckoutUri(request: PaymentRequest): String {
        val dto = CieloRequestDto(
            accessToken = credentials.accessToken,
            clientID = credentials.clientId,
            reference = request.reference,
            email = request.email,
            installments = request.installments,
            items = request.items.map {
                CieloItemDto(
                    name = it.name,
                    quantity = it.quantity,
                    sku = it.sku,
                    unitOfMeasure = it.unitOfMeasure,
                    unitPrice = it.unitPriceInCents,
                )
            },
            paymentCode = request.paymentCode.wireValue,
            value = request.totalInCents.toString(),
        )
        val base64 = encode(json.encodeToString(dto))
        return "$SCHEME_PAYMENT?request=$base64&urlCallback=$CALLBACK"
    }

    /** URI de cancelamento/estorno: lio://payment-reversal?request=<base64>&urlCallback=... */
    fun buildReversalUri(request: CancellationRequest): String {
        val payload = buildString {
            append("{")
            append("\"id\":\"${request.purchaseId}\",")
            append("\"clientID\":\"${credentials.clientId}\",")
            append("\"accessToken\":\"${credentials.accessToken}\",")
            append("\"cieloCode\":\"${request.cieloCode}\",")
            append("\"authCode\":\"${request.authCode}\",")
            append("\"value\":\"${request.totalInCents}\"")
            append("}")
        }
        val base64 = encode(payload)
        return "$SCHEME_REVERSAL?request=$base64&urlCallback=$CALLBACK"
    }

    private fun encode(jsonString: String): String =
        Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)

    private companion object {
        const val SCHEME_PAYMENT = "lio://payment"
        const val SCHEME_REVERSAL = "lio://payment-reversal"
        const val CALLBACK = "order://response"
    }
}
