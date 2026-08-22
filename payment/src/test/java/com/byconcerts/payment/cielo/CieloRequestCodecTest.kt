package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentItem
import com.byconcerts.domain.model.PaymentRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CieloRequestCodecTest {

    private val codec = CieloRequestCodec(
        json = Json { ignoreUnknownKeys = true },
        credentials = CieloCredentials(clientId = "CID", accessToken = "TOKEN"),
    )

    @Test
    fun `checkout URI carrega reference, valor em centavos e callback`() {
        val request = PaymentRequest(
            reference = "key-123",
            paymentCode = PaymentCode.CREDITO_AVISTA,
            totalInCents = 15000,
            items = listOf(PaymentItem(name = "Show", quantity = 3, sku = "evt-1", unitPriceInCents = 5000)),
        )

        val uri = codec.buildCheckoutUri(request)

        assertThat(uri).startsWith("lio://payment?request=")
        assertThat(uri).contains("urlCallback=order://response")

        // Decodifica o request embutido e confere os campos essenciais.
        val requestParam = Uri.parse(uri).getQueryParameter("request")
        val decoded = String(Base64.decode(requestParam, Base64.DEFAULT))
        assertThat(decoded).contains("\"reference\":\"key-123\"")
        assertThat(decoded).contains("\"value\":\"15000\"")
        assertThat(decoded).contains("\"paymentCode\":\"CREDITO_AVISTA\"")
        assertThat(decoded).contains("\"clientID\":\"CID\"")
    }
}
