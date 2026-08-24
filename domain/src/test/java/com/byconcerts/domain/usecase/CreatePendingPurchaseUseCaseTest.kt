package com.byconcerts.domain.usecase

import com.byconcerts.core.common.AppResult
import com.byconcerts.core.common.Clock
import com.byconcerts.core.common.IdGenerator
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.event
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.repository.EventRepository
import com.byconcerts.domain.repository.PurchaseRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreatePendingPurchaseUseCaseTest {

    private val eventRepo = mockk<EventRepository>()
    private val purchaseRepo = mockk<PurchaseRepository>()

    private val ids = ArrayDeque(listOf("pur-1", "key-1"))
    private val useCase = CreatePendingPurchaseUseCase(
        eventRepository = eventRepo,
        purchaseRepository = purchaseRepo,
        clock = Clock { 1234L },
        idGenerator = IdGenerator { ids.removeFirst() },
    )

    @Test
    fun `cria compra PENDING com chave de idempotencia e total correto`() = runTest {
        coEvery { eventRepo.getEvent("evt-1") } returns event(unitPriceInCents = 5000, availableQuantity = 10)
        val captured = slot<com.byconcerts.domain.model.Purchase>()
        coEvery { purchaseRepo.createPendingIfAbsent(capture(captured)) } answers { captured.captured }

        val result = useCase("evt-1", quantity = 3, paymentCode = PaymentCode.CREDITO_AVISTA)

        result as AppResult.Success
        assertThat(result.data.status).isEqualTo(PurchaseStatus.PENDING)
        assertThat(result.data.idempotencyKey).isEqualTo("key-1")
        assertThat(result.data.id).isEqualTo("pur-1")
        assertThat(result.data.totalInCents).isEqualTo(15000)
    }

    @Test
    fun `quantidade invalida retorna InvalidQuantity`() = runTest {
        val result = useCase("evt-1", quantity = 0, paymentCode = PaymentCode.CREDITO_AVISTA)
        result as AppResult.Failure
        assertThat(result.error).isEqualTo(DomainError.InvalidQuantity)
    }

    @Test
    fun `evento inexistente retorna EventNotFound`() = runTest {
        coEvery { eventRepo.getEvent("evt-x") } returns null
        val result = useCase("evt-x", quantity = 1, paymentCode = PaymentCode.CREDITO_AVISTA)
        result as AppResult.Failure
        assertThat(result.error).isInstanceOf(DomainError.EventNotFound::class.java)
    }

    @Test
    fun `quantidade acima do estoque retorna OutOfStock`() = runTest {
        coEvery { eventRepo.getEvent("evt-1") } returns event(availableQuantity = 2)
        val result = useCase("evt-1", quantity = 5, paymentCode = PaymentCode.CREDITO_AVISTA)
        result as AppResult.Failure
        assertThat(result.error).isInstanceOf(DomainError.OutOfStock::class.java)
    }
}
