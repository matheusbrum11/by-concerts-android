package com.byconcerts.domain.usecase

import com.byconcerts.core.common.AppResult
import com.byconcerts.core.common.Clock
import com.byconcerts.core.common.IdGenerator
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.error.DomainResult
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.repository.EventRepository
import com.byconcerts.domain.repository.PurchaseRepository

class CreatePendingPurchaseUseCase(
    private val eventRepository: EventRepository,
    private val purchaseRepository: PurchaseRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        eventId: String,
        quantity: Int,
        paymentCode: PaymentCode,
    ): DomainResult<Purchase> {
        if (quantity <= 0) return AppResult.Failure(DomainError.InvalidQuantity)

        val event = eventRepository.getEvent(eventId)
            ?: return AppResult.Failure(DomainError.EventNotFound(eventId))

        if (quantity > event.availableQuantity) {
            return AppResult.Failure(
                DomainError.OutOfStock(eventId, quantity, event.availableQuantity),
            )
        }

        val now = clock.nowMillis()
        val purchase = Purchase(
            id = idGenerator.newId(),
            idempotencyKey = idGenerator.newId(),
            eventId = event.id,
            eventTitle = event.title,
            quantity = quantity,
            unitPriceInCents = event.unitPriceInCents,
            totalInCents = event.totalInCents(quantity),
            status = PurchaseStatus.PENDING,
            paymentCode = paymentCode,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )

        val stored = purchaseRepository.createPendingIfAbsent(purchase)
        return AppResult.Success(stored)
    }
}
