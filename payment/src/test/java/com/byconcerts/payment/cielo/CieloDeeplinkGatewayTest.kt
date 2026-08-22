package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.PaymentItem
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CieloDeeplinkGatewayTest {

    private val request = PaymentRequest(
        reference = "key-1",
        paymentCode = PaymentCode.CREDITO_AVISTA,
        totalInCents = 10000,
        items = listOf(PaymentItem("Show", 2, "evt-1", 5000)),
    )

    private val codec = mockk<CieloRequestCodec> {
        every { buildCheckoutUri(any()) } returns "lio://payment?request=abc&urlCallback=order://response"
    }

    @Test
    fun `app Cielo ausente retorna Error GatewayNotAvailable`() = runTest {
        val gateway = CieloDeeplinkGateway(codec, launcher = { false }, callbackBus = CieloCallbackBus())

        val result = gateway.pay(request)

        result as PaymentResult.Error
        assertThat(result.type).isEqualTo(PaymentError.GatewayNotAvailable)
    }

    @Test
    fun `callback publicado no barramento resolve o pagamento`() = runTest {
        val bus = CieloCallbackBus()
        val gateway = CieloDeeplinkGateway(codec, launcher = { true }, callbackBus = bus)
        val approved = PaymentResult.Approved(
            PaymentInfo("A", "N", "VISA", "**** 1", 10000),
        )

        val deferred = async { gateway.pay(request) }
        runCurrent() // pay() arma o barramento e fica aguardando
        bus.publish(approved)

        assertThat(deferred.await()).isEqualTo(approved)
    }

    @Test
    fun `sem callback dentro do timeout retorna Error Timeout`() = runTest {
        val gateway = CieloDeeplinkGateway(
            codec,
            launcher = { true },
            callbackBus = CieloCallbackBus(),
            timeoutMillis = 1_000,
        )

        val deferred = async { gateway.pay(request) }
        advanceTimeBy(1_001)
        runCurrent()

        val result = deferred.await()
        result as PaymentResult.Error
        assertThat(result.type).isEqualTo(PaymentError.Timeout)
    }
}
