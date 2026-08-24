package com.byconcerts.feature.checkout

import app.cash.turbine.test
import com.byconcerts.core.common.AppResult
import com.byconcerts.domain.model.Event
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.usecase.BuildPaymentRequestUseCase
import com.byconcerts.domain.usecase.CreatePendingPurchaseUseCase
import com.byconcerts.domain.usecase.ObserveEventUseCase
import com.byconcerts.domain.usecase.ObservePurchaseUseCase
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase
import com.byconcerts.feature.checkout.checkout.CheckoutEffect
import com.byconcerts.feature.checkout.checkout.CheckoutIntent
import com.byconcerts.feature.checkout.checkout.CheckoutPhase
import com.byconcerts.feature.checkout.checkout.CheckoutViewModel
import com.byconcerts.payment.gateway.PaymentGateway
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeEvent = mockk<ObserveEventUseCase>()
    private val observePurchase = mockk<ObservePurchaseUseCase>(relaxed = true)
    private val createPending = mockk<CreatePendingPurchaseUseCase>()
    private val reconcile = mockk<ReconcilePaymentUseCase>()
    private val gateway = mockk<PaymentGateway>()
    private val buildRequest = BuildPaymentRequestUseCase()

    private val event = Event("evt-1", "Show", "Arena", "SP", 1L, 5000, 10, null)
    private val pending = purchase(status = PurchaseStatus.PENDING)

    private fun buildViewModel() = CheckoutViewModel(
        eventId = "evt-1",
        quantity = 2,
        observeEvent = observeEvent,
        observePurchase = observePurchase,
        createPendingPurchase = createPending,
        buildPaymentRequest = buildRequest,
        reconcilePayment = reconcile,
        paymentGateway = gateway,
    ).also { every { observeEvent("evt-1") } returns flowOf(event) }

    @Test
    fun `caminho feliz aprova e emite OpenReceipt`() = runTest {
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase(any()) } returns emptyFlow()
        coEvery { createPending("evt-1", 2, PaymentCode.CREDITO_AVISTA) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Approved(paymentInfo())
        coEvery { reconcile("key-1", any()) } returns
            AppResult.Success(purchase(status = PurchaseStatus.APPROVED, payment = paymentInfo()))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(CheckoutIntent.PayClicked)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(CheckoutEffect.OpenReceipt("pur-1"))
        }
        assertThat(viewModel.state.value.phase).isEqualTo(CheckoutPhase.Approved)
    }

    @Test
    fun `duplo clique nao cria duas compras nem dispara dois pagamentos`() = runTest {
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase(any()) } returns emptyFlow()
        coEvery { createPending(any(), any(), any()) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Approved(paymentInfo())
        coEvery { reconcile(any(), any()) } returns
            AppResult.Success(purchase(status = PurchaseStatus.APPROVED, payment = paymentInfo()))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        // Dois cliques antes de o pagamento processar: o guard bloqueia o segundo.
        viewModel.onIntent(CheckoutIntent.PayClicked)
        viewModel.onIntent(CheckoutIntent.PayClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { createPending(any(), any(), any()) }
        coVerify(exactly = 1) { gateway.pay(any()) }
    }

    @Test
    fun `pagamento negado leva a fase Failed`() = runTest {
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase(any()) } returns emptyFlow()
        coEvery { createPending(any(), any(), any()) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Denied("Sem saldo")
        coEvery { reconcile(any(), any()) } returns
            AppResult.Success(purchase(status = PurchaseStatus.DENIED).copy(failureReason = "Sem saldo"))

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PayClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.phase).isInstanceOf(CheckoutPhase.Failed::class.java)
    }

    @Test
    fun `apos desfecho terminal, nova tentativa cria compra com NOVA chave`() = runTest {
        // Retry com a mesma chave só vale enquanto PENDING. Depois de um
        // cancelamento (terminal), reusar a chave faria a conciliação virar
        // no-op e prenderia o usuário no resultado antigo.
        val canceled = purchase(status = PurchaseStatus.CANCELED)
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase(any()) } returns emptyFlow()
        coEvery { createPending(any(), any(), any()) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Canceled
        coEvery { reconcile(any(), any()) } returns AppResult.Success(canceled)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PayClicked)   // 1a tentativa -> CANCELED
        advanceUntilIdle()
        assertThat(viewModel.state.value.phase).isInstanceOf(CheckoutPhase.Failed::class.java)

        viewModel.onIntent(CheckoutIntent.PayClicked)   // 2a tentativa
        advanceUntilIdle()

        // Duas compras criadas: a tentativa terminal não é reaproveitada.
        coVerify(exactly = 2) { createPending(any(), any(), any()) }
    }

    @Test
    fun `enquanto PENDING, nova tentativa reusa a MESMA chave de idempotencia`() = runTest {
        val stillPending = purchase(status = PurchaseStatus.PENDING)
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase(any()) } returns emptyFlow()
        coEvery { createPending(any(), any(), any()) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Error(PaymentError.Timeout)
        coEvery { reconcile(any(), any()) } returns AppResult.Success(stillPending)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PayClicked)
        advanceUntilIdle()
        viewModel.onIntent(CheckoutIntent.PayClicked)
        advanceUntilIdle()

        // Uma única compra: a segunda tentativa reaproveita a chave PENDING.
        coVerify(exactly = 1) { createPending(any(), any(), any()) }
        coVerify(exactly = 2) { gateway.pay(match { it.reference == "key-1" }) }
    }

    @Test
    fun `desfecho persistido pelo callback aprova mesmo se o gateway der timeout`() = runTest {
        // Cenário real: o processo do app foi morto enquanto o app da Cielo
        // estava em primeiro plano. O gateway em memória não recebe nada
        // (timeout), mas o PaymentCallbackHandler já conciliou e persistiu a
        // compra — que o ViewModel observa do Room.
        val approved = purchase(status = PurchaseStatus.APPROVED, payment = paymentInfo())
        every { observeEvent("evt-1") } returns flowOf(event)
        every { observePurchase("pur-1") } returns flowOf(approved)
        coEvery { createPending(any(), any(), any()) } returns AppResult.Success(pending)
        coEvery { gateway.pay(any()) } returns PaymentResult.Error(PaymentError.Timeout)
        coEvery { reconcile(any(), any()) } returns AppResult.Success(approved)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(CheckoutIntent.PayClicked)
            advanceUntilIdle()
            // Navega ao comprovante UMA única vez, apesar de o desfecho chegar
            // pela observação do Room e pelo retorno do gateway.
            assertThat(awaitItem()).isEqualTo(CheckoutEffect.OpenReceipt("pur-1"))
            expectNoEvents()
        }
        assertThat(viewModel.state.value.phase).isEqualTo(CheckoutPhase.Approved)
    }

    private fun purchase(status: PurchaseStatus, payment: PaymentInfo? = null) = Purchase(
        id = "pur-1",
        idempotencyKey = "key-1",
        eventId = "evt-1",
        eventTitle = "Show",
        quantity = 2,
        unitPriceInCents = 5000,
        totalInCents = 10000,
        status = status,
        paymentCode = PaymentCode.CREDITO_AVISTA,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
        payment = payment,
    )

    private fun paymentInfo() = PaymentInfo("A1", "N1", "VISA", "**** 9", 10000)
}
