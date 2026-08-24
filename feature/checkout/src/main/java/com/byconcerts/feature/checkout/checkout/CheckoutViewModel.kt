package com.byconcerts.feature.checkout.checkout

import androidx.lifecycle.viewModelScope
import com.byconcerts.core.common.AppResult
import com.byconcerts.core.ui.mvi.MviViewModel
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.usecase.BuildPaymentRequestUseCase
import com.byconcerts.domain.usecase.CreatePendingPurchaseUseCase
import com.byconcerts.domain.usecase.ObserveEventUseCase
import com.byconcerts.domain.usecase.ObservePurchaseUseCase
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase
import com.byconcerts.payment.gateway.PaymentGateway
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Orquestra o checkout: cria a compra PENDING, dispara o gateway e reflete o
 * desfecho.
 *
 * Prevenção de cobrança duplicada em DUAS camadas:
 *  1) Guard de estado (aqui): enquanto [CheckoutPhase.Processing], novos
 *     PayClicked são ignorados — o botão não redispara a intent.
 *  2) Persistência: a chave de idempotência é UNIQUE no Room e
 *     [ReconcilePaymentUseCase] trata callback repetido como no-op.
 *
 * Resiliência a morte de processo: a compra é OBSERVADA do Room. Quem persiste
 * o desfecho é o callback do deeplink (PaymentCallbackHandler), então mesmo que
 * este ViewModel tenha sido destruído enquanto o app da Cielo estava em
 * primeiro plano, ao voltar a tela lê o estado real do banco. O retorno do
 * `gateway.pay()` é apenas o atalho do caminho feliz.
 */
class CheckoutViewModel(
    private val eventId: String,
    private val quantity: Int,
    private val observeEvent: ObserveEventUseCase,
    private val observePurchase: ObservePurchaseUseCase,
    private val createPendingPurchase: CreatePendingPurchaseUseCase,
    private val buildPaymentRequest: BuildPaymentRequestUseCase,
    private val reconcilePayment: ReconcilePaymentUseCase,
    private val paymentGateway: PaymentGateway,
) : MviViewModel<CheckoutState, CheckoutIntent, CheckoutEffect>(
    CheckoutState(quantity = quantity),
) {

    private var purchaseObserver: Job? = null

    init {
        viewModelScope.launch {
            val event = observeEvent(eventId).first()
            setState { copy(isLoading = false, event = event) }
        }
    }

    override fun onIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.PaymentCodeChanged ->
                setState { copy(paymentCode = intent.paymentCode) }

            CheckoutIntent.PayClicked -> pay()
        }
    }

    private fun pay() {
        val state = currentState
        // Guard de idempotência a nível de estado: nada de redisparo.
        if (state.phase == CheckoutPhase.Processing || state.phase == CheckoutPhase.Approved) return
        if (state.event == null) return

        setState { copy(phase = CheckoutPhase.Processing, errorMessage = null) }

        viewModelScope.launch {
            // Retry SEMPRE com a mesma chave enquanto a tentativa segue PENDING
            // (pode haver cobrança em aberto do outro lado). Após um desfecho
            // TERMINAL (negado/cancelado), a tentativa acabou: uma nova compra
            // exige uma nova chave, senão a conciliação seria no-op e o usuário
            // ficaria preso no resultado antigo.
            val retryable = state.purchase?.takeIf { it.isPending }
            val purchase = retryable ?: when (
                val result = createPendingPurchase(eventId, quantity, state.paymentCode)
            ) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    failWith(result.error.toMessage())
                    return@launch
                }
            }
            setState { copy(purchase = purchase) }
            observePurchase(purchase.id)

            val paymentResult = paymentGateway.pay(buildPaymentRequest(purchase))

            // O callback já pode ter conciliado e persistido; como o use case é
            // idempotente, esta chamada vira no-op nesse caso.
            when (val reconciled = reconcilePayment(purchase.idempotencyKey, paymentResult)) {
                is AppResult.Success -> onAttemptFinished(reconciled.data)
                is AppResult.Failure -> failWith(reconciled.error.toMessage())
            }
        }
    }

    /**
     * Fim de uma tentativa de pagamento. Diferente da observação do banco, aqui
     * NUNCA podemos continuar em [CheckoutPhase.Processing]: se o desfecho não
     * foi terminal, a tentativa acabou sem conclusão e o usuário precisa poder
     * retentar — deixar o botão travado prenderia a tela para sempre.
     */
    private fun onAttemptFinished(purchase: Purchase) {
        applyOutcome(purchase)
        if (purchase.isPending) {
            val message = purchase.failureReason ?: "Pagamento não concluído. Tente novamente."
            setState { copy(phase = CheckoutPhase.PendingRetry(message)) }
            sendEffect(CheckoutEffect.ShowMessage(message))
        }
    }

    /** Passa a refletir o estado persistido da compra (fonte de verdade). */
    private fun observePurchase(purchaseId: String) {
        purchaseObserver?.cancel()
        purchaseObserver = observePurchase.invoke(purchaseId)
            .filterNotNull()
            .onEach(::applyOutcome)
            .launchIn(viewModelScope)
    }

    private fun applyOutcome(purchase: Purchase) {
        val alreadyApproved = currentState.phase == CheckoutPhase.Approved
        setState { copy(purchase = purchase) }

        when (purchase.status) {
            PurchaseStatus.APPROVED -> {
                setState { copy(phase = CheckoutPhase.Approved) }
                // Navega uma única vez, ainda que o desfecho chegue pelos dois
                // caminhos (retorno do gateway + observação do Room).
                if (!alreadyApproved) sendEffect(CheckoutEffect.OpenReceipt(purchase.id))
            }

            PurchaseStatus.DENIED -> setState {
                copy(phase = CheckoutPhase.Failed(purchase.failureReason ?: "Pagamento negado."))
            }

            PurchaseStatus.CANCELED -> setState {
                copy(phase = CheckoutPhase.Failed(purchase.failureReason ?: "Pagamento cancelado."))
            }

            // Ainda PENDING vindo da OBSERVAÇÃO do banco: pode ser simplesmente
            // a compra recém-criada, com o pagamento em andamento. Não mexemos
            // na fase aqui — quem encerra a tentativa é [onAttemptFinished].
            PurchaseStatus.PENDING -> Unit
        }
    }

    private fun failWith(message: String) {
        setState { copy(phase = CheckoutPhase.PendingRetry(message), errorMessage = message) }
        sendEffect(CheckoutEffect.ShowMessage(message))
    }
}

private fun DomainError.toMessage(): String = when (this) {
    is DomainError.EventNotFound -> "Evento não encontrado."
    is DomainError.OutOfStock -> "Estoque insuficiente: restam $available ingresso(s)."
    DomainError.InvalidQuantity -> "Quantidade inválida."
    DomainError.PurchaseNotFound -> "Compra não encontrada."
    is DomainError.Storage -> "Falha ao salvar os dados da compra."
    is DomainError.Payment -> "Falha no pagamento."
}
