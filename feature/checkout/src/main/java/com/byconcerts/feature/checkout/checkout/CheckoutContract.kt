package com.byconcerts.feature.checkout.checkout

import com.byconcerts.domain.model.Event
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.Purchase

/** Fase do checkout — reflete explicitamente cada estado do pagamento. */
sealed interface CheckoutPhase {
    /** Pronto para pagar. */
    data object Idle : CheckoutPhase

    /** Pagamento em andamento: o botão fica desabilitado (guard de idempotência). */
    data object Processing : CheckoutPhase

    /** Aprovado — segue para o comprovante. */
    data object Approved : CheckoutPhase

    /** Desfecho negativo definitivo (negado/cancelado). */
    data class Failed(val message: String) : CheckoutPhase

    /** Erro transitório ou pagamento parcial: pode retentar com a MESMA chave. */
    data class PendingRetry(val message: String) : CheckoutPhase
}

data class CheckoutState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val quantity: Int = 1,
    val paymentCode: PaymentCode = PaymentCode.CREDITO_AVISTA,
    val purchase: Purchase? = null,
    val phase: CheckoutPhase = CheckoutPhase.Idle,
    val errorMessage: String? = null,
) {
    val totalInCents: Long get() = event?.totalInCents(quantity) ?: 0L

    /** O botão de pagar só é habilitado quando não há pagamento em curso. */
    val isPayEnabled: Boolean
        get() = event != null && phase != CheckoutPhase.Processing && phase != CheckoutPhase.Approved
}

sealed interface CheckoutIntent {
    data class PaymentCodeChanged(val paymentCode: PaymentCode) : CheckoutIntent
    data object PayClicked : CheckoutIntent
}

sealed interface CheckoutEffect {
    data class OpenReceipt(val purchaseId: String) : CheckoutEffect
    data class ShowMessage(val message: String) : CheckoutEffect
}
