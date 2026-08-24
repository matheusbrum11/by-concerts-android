package com.byconcerts.domain.usecase

import com.byconcerts.core.common.AppResult
import com.byconcerts.core.common.Clock
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.paymentInfo
import com.byconcerts.domain.purchase
import com.byconcerts.domain.repository.EventRepository
import com.byconcerts.domain.repository.PurchaseRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReconcilePaymentUseCaseTest {

    private val purchaseRepo = mockk<PurchaseRepository>(relaxed = true)
    private val eventRepo = mockk<EventRepository>(relaxed = true)
    private val useCase = ReconcilePaymentUseCase(purchaseRepo, eventRepo, Clock { 999L })

    @Test
    fun `aprovado marca APPROVED, guarda pagamento e baixa estoque`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-1") } returns purchase()

        val result = useCase("key-1", PaymentResult.Approved(paymentInfo()))

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.APPROVED)
        assertThat(result.data.payment).isEqualTo(paymentInfo())
        coVerify(exactly = 1) { purchaseRepo.update(match { it.status == PurchaseStatus.APPROVED }) }
        coVerify(exactly = 1) { eventRepo.decrementAvailability("evt-1", 2) }
    }

    @Test
    fun `negado marca DENIED e nao baixa estoque`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-1") } returns purchase()

        val result = useCase("key-1", PaymentResult.Denied(reason = "Sem saldo", code = 57))

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.DENIED)
        assertThat(result.data.failureReason).isEqualTo("Sem saldo")
        coVerify(exactly = 0) { eventRepo.decrementAvailability(any(), any()) }
    }

    @Test
    fun `cancelado marca CANCELED`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-1") } returns purchase()

        val result = useCase("key-1", PaymentResult.Canceled)

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.CANCELED)
    }

    @Test
    fun `erro transitorio mantem PENDING para retry com a mesma chave`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-1") } returns purchase()

        val result = useCase("key-1", PaymentResult.Error(PaymentError.Timeout))

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.PENDING)
        coVerify(exactly = 0) { eventRepo.decrementAvailability(any(), any()) }
    }

    @Test
    fun `callback duplicado em compra ja terminal e no-op idempotente`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-1") } returns
            purchase(status = PurchaseStatus.APPROVED, payment = paymentInfo())

        val result = useCase("key-1", PaymentResult.Approved(paymentInfo()))

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.APPROVED)
        coVerify(exactly = 0) { purchaseRepo.update(any()) }
        coVerify(exactly = 0) { eventRepo.decrementAvailability(any(), any()) }
    }

    @Test
    fun `compra inexistente retorna PurchaseNotFound`() = runTest {
        coEvery { purchaseRepo.findByIdempotencyKey("key-x") } returns null

        val result = useCase("key-x", PaymentResult.Approved(paymentInfo()))

        result as AppResult.Failure
        assertThat(result.error).isEqualTo(DomainError.PurchaseNotFound)
    }
}
