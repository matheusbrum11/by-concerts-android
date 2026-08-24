package com.byconcerts.domain.usecase

import com.byconcerts.domain.model.PaymentItem
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.Purchase

class BuildPaymentRequestUseCase {
    operator fun invoke(purchase: Purchase): PaymentRequest = PaymentRequest(
        reference = purchase.idempotencyKey,
        paymentCode = purchase.paymentCode,
        totalInCents = purchase.totalInCents,
        items = listOf(
            PaymentItem(
                name = purchase.eventTitle,
                quantity = purchase.quantity,
                sku = purchase.eventId,
                unitPriceInCents = purchase.unitPriceInCents,
            ),
        ),
    )
}
