package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.PaymentCallback
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class CieloDeeplinkGatewayTest {

    private val request = PaymentRequest(
        reference = "key-1",
        paymentCode = PaymentCode.CREDITO_AVISTA,
        totalInCents = 10000,
        items = listOf(PaymentItem("Show", 2, "evt-1", 5000)),
    )

    private val codec = mockk<CieloRequestCodec> {
        every { buildCheckoutUri(any()) } returns "lio://payment?request=abc&urlCallback=order%3A%2F%2Fpayment"
    }

    private fun launcher(available: Boolean = true, launches: Boolean = true) =
        object : DeeplinkLauncher {
            override fun launch(uri: String) = launches
            override fun isPaymentAppAvailable() = available
        }

    @Test
    fun `app da Cielo nao instalado retorna GatewayNotAvailable sem disparar intent`() = runTest {
        val gateway = CieloDeeplinkGateway(codec, launcher(available = false), CieloCallbackBus())

        val result = gateway.pay(request)

        assertThat((result as PaymentResult.Error).type).isEqualTo(PaymentError.GatewayNotAvailable)
    }

    @Test
    fun `falha ao disparar a intent retorna GatewayNotAvailable`() = runTest {
        val gateway = CieloDeeplinkGateway(codec, launcher(launches = false), CieloCallbackBus())

        val result = gateway.pay(request)

        assertThat((result as PaymentResult.Error).type).isEqualTo(PaymentError.GatewayNotAvailable)
    }

    @Test
    fun `callback publicado no barramento resolve o pagamento`() = runTest {
        val bus = CieloCallbackBus()
        val gateway = CieloDeeplinkGateway(codec, launcher(), bus)
        val approved = PaymentResult.Approved(PaymentInfo("A", "N", "VISA", "**** 1", 10000))

        val deferred = async { gateway.pay(request) }
        runCurrent()
        bus.publish(PaymentCallback(reference = "key-1", result = approved))

        assertThat(deferred.await()).isEqualTo(approved)
    }

    @Test
    fun `sem callback dentro do timeout retorna Error Timeout`() = runTest {
        val gateway = CieloDeeplinkGateway(
            codec,
            launcher(),
            CieloCallbackBus(),
            timeoutMillis = 1_000,
        )

        val deferred = async { gateway.pay(request) }
        advanceTimeBy(1_001.milliseconds)
        runCurrent()

        assertThat((deferred.await() as PaymentResult.Error).type).isEqualTo(PaymentError.Timeout)
    }
}
