package com.byconcerts.domain.model

enum class PurchaseStatus { PENDING, APPROVED, DENIED, CANCELED }

data class Purchase(
    val id: String,
    val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val totalInCents: Long,
    val status: PurchaseStatus,
    val paymentCode: PaymentCode,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val payment: PaymentInfo? = null,
    val failureReason: String? = null,
) {
    val isPending: Boolean get() = status == PurchaseStatus.PENDING
    val isTerminal: Boolean get() = status != PurchaseStatus.PENDING
}
