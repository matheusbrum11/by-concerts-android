package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentRequest
import kotlinx.serialization.json.Json

/**
 * Monta as URIs de deeplink da Cielo, seguindo o sample oficial.
 *
 * Todo valor monetário trafega em CENTAVOS (numérico, sem vírgula). O JSON é
 * serializado em UTF-8 e convertido para BASE64 com `NO_WRAP`.
 *
 * A URI é construída com [Uri.Builder] — e não por concatenação — porque o
 * Base64 padrão contém `+`, `/` e `=`, que precisam ser percent-encoded no
 * query param. Concatenar faria o `+` chegar ao app da Cielo como espaço,
 * corrompendo o payload.
 */
class CieloRequestCodec(
    private val json: Json,
    private val credentials: CieloCredentials,
) {

    /** `lio://payment?request=<base64>&urlCallback=order://payment` */
    fun buildCheckoutUri(request: PaymentRequest): String {
        val dto = CieloRequestDto(
            accessToken = credentials.accessToken,
            clientId = credentials.clientId,
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
            merchantCode = request.merchantCode.ifEmpty { credentials.merchantCode },
            paymentCode = request.paymentCode.wireValue,
            value = request.totalInCents,
        )
        return buildUri(AUTHORITY_PAYMENT, json.encodeToString(CieloRequestDto.serializer(), dto))
    }

    /** `lio://payment-reversal?request=<base64>&urlCallback=order://payment` */
    fun buildReversalUri(request: CancellationRequest): String {
        val dto = CieloReversalRequestDto(
            id = request.purchaseId,
            clientId = credentials.clientId,
            accessToken = credentials.accessToken,
            cieloCode = request.cieloCode,
            authCode = request.authCode,
            value = request.totalInCents,
        )
        return buildUri(
            AUTHORITY_REVERSAL,
            json.encodeToString(CieloReversalRequestDto.serializer(), dto),
        )
    }

    private fun buildUri(authority: String, payload: String): String {
        val base64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(authority)
            .appendQueryParameter(PARAM_REQUEST, base64)
            .appendQueryParameter(PARAM_URL_CALLBACK, CALLBACK_URL)
            .build()
            .toString()
    }

    companion object {
        const val SCHEME = "lio"
        const val AUTHORITY_PAYMENT = "payment"
        const val AUTHORITY_REVERSAL = "payment-reversal"

        /** Contrato de retorno declarado no AndroidManifest (scheme://host). */
        const val CALLBACK_SCHEME = "order"
        const val CALLBACK_HOST = "payment"
        const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"

        private const val PARAM_REQUEST = "request"
        private const val PARAM_URL_CALLBACK = "urlCallback"
    }
}
