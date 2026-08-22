package com.byconcerts.domain.model

/**
 * Evento à venda. Valores monetários SEMPRE em centavos (Long) — mesma
 * representação canônica do Design System (MnsCurrencyFormatter) e da Cielo.
 */
data class Event(
    val id: String,
    val title: String,
    val venue: String,
    val city: String,
    val dateEpochMillis: Long,
    val unitPriceInCents: Long,
    val availableQuantity: Int,
    val imageUrl: String?,
    val description: String? = null,
) {
    val isSoldOut: Boolean get() = availableQuantity <= 0

    /** Total em centavos para uma dada quantidade. */
    fun totalInCents(quantity: Int): Long = unitPriceInCents * quantity
}
