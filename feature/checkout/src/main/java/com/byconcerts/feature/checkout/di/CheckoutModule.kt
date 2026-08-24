package com.byconcerts.feature.checkout.di

import com.byconcerts.feature.checkout.checkout.CheckoutViewModel
import com.byconcerts.feature.checkout.receipt.ReceiptViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val checkoutModule = module {
    viewModel { (eventId: String, quantity: Int) ->
        CheckoutViewModel(
            eventId = eventId,
            quantity = quantity,
            observeEvent = get(),
            observePurchase = get(),
            createPendingPurchase = get(),
            buildPaymentRequest = get(),
            reconcilePayment = get(),
            paymentGateway = get(),
        )
    }
    viewModel { (purchaseId: String) -> ReceiptViewModel(purchaseId, get()) }
}
