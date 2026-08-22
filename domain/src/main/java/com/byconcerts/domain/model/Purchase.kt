package com.byconcerts.domain.model

/** Estado de uma compra ao longo do ciclo de pagamento. */
enum class PurchaseStatus { PENDING, APPROVED, DENIED, CANCELED }

/**
 * Compra de ingressos. Criada como [PurchaseStatus.PENDING] ANTES de disparar o
 * pagamento e conciliada quando o callback chega.
 *
 * @param id identificador da compra (UUID). É o conteúdo do QR Code do ingresso.
 * @param idempotencyKey chave única da tentativa; usada como `reference` na Cielo
 *   e protegida por constraint UNIQUE no banco — o coração da não-duplicação.
 * @param payment dados retornados pela Cielo; presente apenas quando APPROVED.
 */
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
    val isApproved: Boolean get() = status == PurchaseStatus.APPROVED

    /** Estados terminais não devem ser reprocessados (idempotência do callback). */
    val isTerminal: Boolean get() = status != PurchaseStatus.PENDING
}
