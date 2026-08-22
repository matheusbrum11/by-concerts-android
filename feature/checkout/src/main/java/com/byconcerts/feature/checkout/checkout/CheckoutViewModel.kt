package com.byconcerts.feature.checkout.checkout

import androidx.lifecycle.viewModelScope
import com.byconcerts.core.common.fold
import com.byconcerts.core.ui.mvi.MviViewModel
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.usecase.BuildPaymentRequestUseCase
import com.byconcerts.domain.usecase.CreatePendingPurchaseUseCase
import com.byconcerts.domain.usecase.ObserveEventUseCase
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase
import com.byconcerts.payment.gateway.PaymentGateway
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Orquestra o checkout: cria a compra PENDING, dispara o gateway e concilia o
 * resultado. A prevenção de cobrança duplicada acontece em DUAS camadas:
 *
 *  1) Guard de UI/estado (aqui): enquanto [CheckoutPhase.Processing], novos
 *     PayClicked são ignorados — o botão não redispara a intent.
 *  2) Persistência (use cases + Room): a chave de idempotência é UNIQUE e o
 *     [ReconcilePaymentUseCase] trata callback repetido como no-op.
 *
 * Em erro transitório/parcial a compra permanece PENDING e o pagamento pode ser
 * retentado com a MESMA chave (mesma compra), nunca criando outra cobrança.
 */
class CheckoutViewModel(
    private val eventId: String,
    private val quantity: Int,
    private val observeEvent: ObserveEventUseCase,
    private val createPendingPurchase: CreatePendingPurchaseUseCase,
    private val buildPaymentRequest: BuildPaymentRequestUseCase,
    private val reconcilePayment: ReconcilePaymentUseCase,
    private val paymentGateway: PaymentGateway,
) : MviViewModel<CheckoutState, CheckoutIntent, CheckoutEffect>(
    CheckoutState(quantity = quantity),
) {

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
            // Reaproveita a compra PENDING existente (retry) ou cria uma nova.
            val purchase = state.purchase ?: when (
                val result = createPendingPurchase(eventId, quantity, state.paymentCode)
            ) {
                is com.byconcerts.core.common.AppResult.Success -> result.data
                is com.byconcerts.core.common.AppResult.Failure -> {
                    failWith(result.error.toMessage())
                    return@launch
                }
            }
            setState { copy(purchase = purchase) }

            val paymentResult = paymentGateway.pay(buildPaymentRequest(purchase))

            reconcilePayment(purchase.idempotencyKey, paymentResult).fold(
                onSuccess = { reconciled -> applyOutcome(reconciled) },
                onFailure = { failWith(it.toMessage()) },
            )
        }
    }

    private fun applyOutcome(purchase: Purchase) {
        setState { copy(purchase = purchase) }
        when (purchase.status) {
            PurchaseStatus.APPROVED -> {
                setState { copy(phase = CheckoutPhase.Approved) }
                sendEffect(CheckoutEffect.OpenReceipt(purchase.id))
            }

            PurchaseStatus.DENIED -> setState {
                copy(phase = CheckoutPhase.Failed(purchase.failureReason ?: "Pagamento negado."))
            }

            PurchaseStatus.CANCELED -> setState {
                copy(phase = CheckoutPhase.Failed(purchase.failureReason ?: "Pagamento cancelado."))
            }

            PurchaseStatus.PENDING -> {
                val message = purchase.failureReason ?: "Pagamento não concluído. Tente novamente."
                setState { copy(phase = CheckoutPhase.PendingRetry(message)) }
                sendEffect(CheckoutEffect.ShowMessage(message))
            }
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
