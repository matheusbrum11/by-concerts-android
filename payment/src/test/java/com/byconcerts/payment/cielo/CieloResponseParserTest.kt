package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentResult
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CieloResponseParserTest {

    private val parser = CieloResponseParser(
        Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true },
    )

    private fun encode(json: String): String =
        Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)

    private fun callbackUri(json: String, responseCode: String? = null): Uri =
        Uri.Builder()
            .scheme("order").authority("payment")
            .appendQueryParameter("response", encode(json))
            .apply { responseCode?.let { appendQueryParameter("responsecode", it) } }
            .build()

    private fun successOrder(
        pendingAmount: Long = 0,
        statusCode: String = "1",
        reference: String = "key-123",
    ) = """
        {
          "createdAt": "2026-08-23T10:00:00Z",
          "id": "ord-987",
          "reference": "$reference",
          "number": "1234",
          "price": 24000,
          "paidAmount": 24000,
          "pendingAmount": $pendingAmount,
          "status": "PAID",
          "type": "SALE",
          "items": [
            { "name": "Rock na Praça", "quantity": 2, "sku": "evt-1",
              "unitOfMeasure": "unidade", "unitPrice": 12000 }
          ],
          "payments": [
            {
              "id": "pay-555",
              "amount": 24000,
              "authCode": "123456",
              "cieloCode": "789012",
              "brand": "MASTERCARD",
              "mask": "************1234",
              "installments": 1,
              "terminal": "PDV001",
              "paymentFields": { "statusCode": "$statusCode", "entranceMode": "CHIP" }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `sucesso autorizado mapeia Order da raiz para Approved com reference`() {
        val callback = parser.parse(callbackUri(successOrder(), responseCode = "0"))

        assertThat(callback.reference).isEqualTo("key-123")
        val result = callback.result as PaymentResult.Approved
        assertThat(result.payment.authCode).isEqualTo("123456")
        assertThat(result.payment.cieloCode).isEqualTo("789012")
        assertThat(result.payment.brand).isEqualTo("MASTERCARD")
        assertThat(result.payment.maskedCard).isEqualTo("************1234")
        assertThat(result.payment.amountInCents).isEqualTo(24000)
        assertThat(result.payment.paymentId).isEqualTo("pay-555")
    }

    @Test
    fun `statusCode chega como STRING e ainda assim e interpretado`() {
        val callback = parser.parse(callbackUri(successOrder(statusCode = "1"), "0"))
        assertThat(callback.result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `PIX (statusCode 0) tambem e aprovado`() {
        val callback = parser.parse(callbackUri(successOrder(statusCode = "0"), "0"))
        assertThat(callback.result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `statusCode 2 (cancelamento) vira Canceled`() {
        val callback = parser.parse(callbackUri(successOrder(statusCode = "2"), "0"))
        assertThat(callback.result).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `pagamento parcial (pendingAmount diferente de zero) NAO e aprovado`() {
        val callback = parser.parse(callbackUri(successOrder(pendingAmount = 5000), "0"))

        assertThat(callback.reference).isEqualTo("key-123")
        val result = callback.result as PaymentResult.Error
        assertThat(result.type).isEqualTo(PaymentError.PartialPayment(5000))
    }

    @Test
    fun `erro com code diferente de 1 vira Denied e preserva a reference da order`() {
        val json = """
            {
              "code": 57,
              "reason": "TRANSACAO NEGADA",
              "order": { "id": "ord-1", "reference": "key-456", "status": "PENDING" }
            }
        """.trimIndent()

        val callback = parser.parse(callbackUri(json))

        assertThat(callback.reference).isEqualTo("key-456")
        val result = callback.result as PaymentResult.Denied
        assertThat(result.code).isEqualTo(57)
        assertThat(result.reason).isEqualTo("TRANSACAO NEGADA")
    }

    @Test
    fun `code 1 (cancelado pelo usuario) vira Canceled`() {
        val json = """
            {
              "code": 1,
              "reason": "CANCELADO PELO USUARIO",
              "order": { "reference": "key-789" }
            }
        """.trimIndent()

        val callback = parser.parse(callbackUri(json))

        assertThat(callback.reference).isEqualTo("key-789")
        assertThat(callback.result).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `base64 com quebras de linha ainda decodifica`() {
        val wrapped = Base64.encodeToString(successOrder().toByteArray(), Base64.DEFAULT)
        val callback = parser.parse(wrapped)
        assertThat(callback.result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `cancelamento real do emulador Cielo (com newline e responsecode=0) vira Canceled`() {
        val realUri = Uri.parse(
            "order://payment?response=eyJjb2RlIjoxLCJyZWFzb24iOiJDQU5DRUxBRE8gUEVMTyBVU1XDgVJJTyJ9" +
                "\n&responsecode=0",
        )

        val callback = parser.parse(realUri)

        assertThat(callback.result).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `sucesso com responsecode ausente ainda e lido como sucesso`() {
        val callback = parser.parse(callbackUri(successOrder(), responseCode = null))
        assertThat(callback.result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `campos desconhecidos no payload nao quebram o parse`() {
        val json = """
            {
              "id": "ord-1", "reference": "key-1", "pendingAmount": 0, "paidAmount": 100,
              "campoNovoDaCielo": "qualquer coisa",
              "payments": [{ "authCode": "A", "cieloCode": "N", "brand": "VISA",
                "mask": "****1", "amount": 100, "outroCampo": 42,
                "paymentFields": { "statusCode": "1", "campoExtra": "x" } }]
            }
        """.trimIndent()

        val callback = parser.parse(callbackUri(json, "0"))
        assertThat(callback.result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `response nulo, vazio ou corrompido vira Error InvalidResponse`() {
        assertThat((parser.parse(null as Uri?).result as PaymentResult.Error).type)
            .isEqualTo(PaymentError.InvalidResponse)
        assertThat((parser.parse("").result as PaymentResult.Error).type)
            .isEqualTo(PaymentError.InvalidResponse)
        assertThat((parser.parse(encode("{isto nao e json")).result as PaymentResult.Error).type)
            .isEqualTo(PaymentError.InvalidResponse)
    }
}
