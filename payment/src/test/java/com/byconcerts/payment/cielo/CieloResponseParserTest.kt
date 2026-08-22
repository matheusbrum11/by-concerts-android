package com.byconcerts.payment.cielo

import android.util.Base64
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentResult
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Mapeamento dos retornos da Cielo → PaymentResult. Roda sob Robolectric por
 * causa do android.util.Base64 usado no decode.
 */
@RunWith(RobolectricTestRunner::class)
class CieloResponseParserTest {

    private val parser = CieloResponseParser(Json { ignoreUnknownKeys = true })

    private fun encode(json: String): String =
        Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)

    @Test
    fun `sucesso autorizado vira Approved com dados de pagamento`() {
        val json = """
            {"payments":[{"authCode":"A1","cieloCode":"N1","brand":"MASTER",
            "mask":"**** 9999","amount":10000,"paymentFields":{"statusCode":1}}],
            "pendingAmount":0}
        """.trimIndent()

        val result = parser.parse(encode(json))

        result as PaymentResult.Approved
        assertThat(result.payment.authCode).isEqualTo("A1")
        assertThat(result.payment.cieloCode).isEqualTo("N1")
        assertThat(result.payment.brand).isEqualTo("MASTER")
        assertThat(result.payment.amountInCents).isEqualTo(10000)
    }

    @Test
    fun `sucesso PIX (statusCode 0) tambem vira Approved`() {
        val json = """{"payments":[{"amount":5000,"paymentFields":{"statusCode":0}}],"pendingAmount":0}"""
        assertThat(parser.parse(encode(json))).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `pagamento parcial (pendingAmount != 0) vira Error PartialPayment`() {
        val json = """{"payments":[{"amount":5000,"paymentFields":{"statusCode":1}}],"pendingAmount":500}"""
        val result = parser.parse(encode(json))
        result as PaymentResult.Error
        assertThat(result.type).isEqualTo(PaymentError.PartialPayment(500))
    }

    @Test
    fun `erro com code diferente de 1 vira Denied`() {
        val json = """{"code":57,"reason":"NEGADO"}"""
        val result = parser.parse(encode(json))
        result as PaymentResult.Denied
        assertThat(result.code).isEqualTo(57)
        assertThat(result.reason).isEqualTo("NEGADO")
    }

    @Test
    fun `cancelamento (code 1) vira Canceled`() {
        val json = """{"code":1,"reason":"CANCELADO PELO USUARIO"}"""
        assertThat(parser.parse(encode(json))).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun `response nulo ou vazio vira Error InvalidResponse`() {
        assertThat((parser.parse(null) as PaymentResult.Error).type)
            .isEqualTo(PaymentError.InvalidResponse)
        assertThat((parser.parse("") as PaymentResult.Error).type)
            .isEqualTo(PaymentError.InvalidResponse)
    }

    @Test
    fun `json corrompido vira Error InvalidResponse`() {
        val result = parser.parse(encode("{isto nao e json valido"))
        result as PaymentResult.Error
        assertThat(result.type).isEqualTo(PaymentError.InvalidResponse)
    }
}
