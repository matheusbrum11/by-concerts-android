package com.byconcerts.payment.cielo

import android.net.Uri
import android.util.Base64
import com.byconcerts.core.common.AppResult
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentCallbackHandlerTest {

    private val reconcile = mockk<ReconcilePaymentUseCase>(relaxed = true)
    private val bus = CieloCallbackBus()
    private val handler = PaymentCallbackHandler(
        parser = CieloResponseParser(Json { ignoreUnknownKeys = true; isLenient = true }),
        reconcilePayment = reconcile,
        callbackBus = bus,
    )

    private fun callbackUri(json: String, responseCode: String? = null): Uri =
        Uri.Builder()
            .scheme("order").authority("payment")
            .appendQueryParameter(
                "response",
                Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP),
            )
            .apply { responseCode?.let { appendQueryParameter("responsecode", it) } }
            .build()

    private val approvedOrder = """
        {
          "id": "ord-1", "reference": "idem-key-1", "status": "PAID",
          "paidAmount": 24000, "pendingAmount": 0,
          "payments": [{ "id": "pay-1", "amount": 24000, "authCode": "123456",
            "cieloCode": "789012", "brand": "VISA", "mask": "****1234",
            "paymentFields": { "statusCode": "1" } }]
        }
    """.trimIndent()

    @Test
    fun `concilia pela reference retornada pela Cielo e persiste o desfecho`() = runTest {
        coEvery { reconcile(any(), any()) } returns AppResult.Failure(mockk(relaxed = true))

        val callback = handler.handle(callbackUri(approvedOrder, "0"))

        assertThat(callback.reference).isEqualTo("idem-key-1")
        coVerify(exactly = 1) {
            reconcile("idem-key-1", match { it is PaymentResult.Approved })
        }
    }

    @Test
    fun `publica no barramento para retomar quem estiver aguardando`() = runTest {
        coEvery { reconcile(any(), any()) } returns AppResult.Failure(mockk(relaxed = true))
        val awaiting = bus.arm()

        handler.handle(callbackUri(approvedOrder, "0"))

        assertThat(awaiting.isCompleted).isTrue()
        assertThat(awaiting.getCompleted().result).isInstanceOf(PaymentResult.Approved::class.java)
    }

    @Test
    fun `callback sem reference nao concilia (nao ha como correlacionar com seguranca)`() = runTest {
        val semReference = """{ "id": "ord-1", "pendingAmount": 0, "paidAmount": 1,
            "payments": [{ "authCode": "A", "paymentFields": { "statusCode": "1" } }] }"""

        val callback = handler.handle(callbackUri(semReference, "0"))

        assertThat(callback.reference).isNull()
        coVerify(exactly = 0) { reconcile(any(), any()) }
    }

    @Test
    fun `callback duplicado chama a conciliacao de novo — e ela e idempotente`() = runTest {
        coEvery { reconcile(any(), any()) } returns AppResult.Failure(mockk(relaxed = true))

        handler.handle(callbackUri(approvedOrder, "0"))
        handler.handle(callbackUri(approvedOrder, "0"))

        coVerify(exactly = 2) { reconcile("idem-key-1", any()) }
    }
}
