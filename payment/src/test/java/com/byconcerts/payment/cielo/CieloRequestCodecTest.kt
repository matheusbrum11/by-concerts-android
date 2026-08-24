package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentItem
import com.byconcerts.domain.model.PaymentRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CieloRequestCodecTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val codec = CieloRequestCodec(
        json = json,
        credentials = CieloCredentials(
            clientId = "CID",
            accessToken = "TOKEN",
            merchantCode = "MERCH",
        ),
    )

    private val request = PaymentRequest(
        reference = "key-123",
        paymentCode = PaymentCode.CREDITO_AVISTA,
        totalInCents = 24000,
        items = listOf(
            PaymentItem(name = "Rock na Praça", quantity = 2, sku = "evt-1", unitPriceInCents = 12000),
        ),
    )

    private fun decodeRequestParam(uri: String) =
        json.parseToJsonElement(
            String(Base64.decode(Uri.parse(uri).getQueryParameter("request"), Base64.DEFAULT)),
        ).jsonObject

    @Test
    fun `monta lio payment com callback order payment`() {
        val uri = Uri.parse(codec.buildCheckoutUri(request))

        assertThat(uri.scheme).isEqualTo("lio")
        assertThat(uri.authority).isEqualTo("payment")
        assertThat(uri.getQueryParameter("urlCallback")).isEqualTo("order://payment")
    }

    @Test
    fun `payload segue os campos do contrato oficial, com valores em centavos`() {
        val payload = decodeRequestParam(codec.buildCheckoutUri(request))

        assertThat(payload["reference"]!!.jsonPrimitive.content).isEqualTo("key-123")
        assertThat(payload["accessToken"]!!.jsonPrimitive.content).isEqualTo("TOKEN")
        assertThat(payload["clientID"]!!.jsonPrimitive.content).isEqualTo("CID")
        assertThat(payload["merchantCode"]!!.jsonPrimitive.content).isEqualTo("MERCH")
        assertThat(payload["paymentCode"]!!.jsonPrimitive.content).isEqualTo("CREDITO_AVISTA")
        assertThat(payload["value"]!!.jsonPrimitive.content).isEqualTo("24000")
        assertThat(payload["installments"]!!.jsonPrimitive.content).isEqualTo("0")

        val item = payload["items"]!!.jsonArrayFirst()
        assertThat(item["sku"]!!.jsonPrimitive.content).isEqualTo("evt-1")
        assertThat(item["quantity"]!!.jsonPrimitive.content).isEqualTo("2")
        assertThat(item["unitPrice"]!!.jsonPrimitive.content).isEqualTo("12000")
        assertThat(item["unitOfMeasure"]!!.jsonPrimitive.content).isEqualTo("unidade")
    }

    @Test
    fun `base64 e percent-encoded na query, sobrevivendo a +, barra e igual`() {
        val longRequest = request.copy(
            items = List(12) { PaymentItem("Item $it ~ áéî", it + 1, "sku-$it", 9999) },
        )
        val uri = codec.buildCheckoutUri(longRequest)

        val raw = uri.substringAfter("request=").substringBefore("&")
        assertThat(raw).doesNotContain("+")

        val payload = decodeRequestParam(uri)
        assertThat(payload["value"]!!.jsonPrimitive.content).isEqualTo("24000")
        assertThat(payload["items"]!!.jsonArrayFirst()["name"]!!.jsonPrimitive.content)
            .isEqualTo("Item 0 ~ áéî")
    }

    @Test
    fun `cancelamento monta lio payment-reversal com os campos do estorno`() {
        val uri = codec.buildReversalUri(
            CancellationRequest(
                purchaseId = "ord-1",
                reference = "key-1",
                cieloCode = "789012",
                authCode = "123456",
                totalInCents = 24000,
            ),
        )

        assertThat(Uri.parse(uri).authority).isEqualTo("payment-reversal")
        val payload = decodeRequestParam(uri)
        assertThat(payload["id"]!!.jsonPrimitive.content).isEqualTo("ord-1")
        assertThat(payload["cieloCode"]!!.jsonPrimitive.content).isEqualTo("789012")
        assertThat(payload["authCode"]!!.jsonPrimitive.content).isEqualTo("123456")
        assertThat(payload["value"]!!.jsonPrimitive.content).isEqualTo("24000")
    }
}

private fun kotlinx.serialization.json.JsonElement.jsonArrayFirst() =
    (this as kotlinx.serialization.json.JsonArray).first().jsonObject
