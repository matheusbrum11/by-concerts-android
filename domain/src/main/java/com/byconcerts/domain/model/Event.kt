package com.byconcerts.domain.model

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

    fun totalInCents(quantity: Int): Long = unitPriceInCents * quantity
}
