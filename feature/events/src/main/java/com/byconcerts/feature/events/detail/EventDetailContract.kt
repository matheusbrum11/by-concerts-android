package com.byconcerts.feature.events.detail

import com.byconcerts.domain.model.Event

data class EventDetailState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val quantity: Int = 1,
    val errorMessage: String? = null,
) {
    val totalInCents: Long get() = event?.totalInCents(quantity) ?: 0L
    val maxQuantity: Int get() = event?.availableQuantity ?: 0
    val canBuy: Boolean get() = event != null && !event.isSoldOut && quantity in 1..maxQuantity
}

sealed interface EventDetailIntent {
    data class QuantityChanged(val quantity: Int) : EventDetailIntent
    data object BuyClicked : EventDetailIntent
}

sealed interface EventDetailEffect {
    data class OpenCheckout(val eventId: String, val quantity: Int) : EventDetailEffect
}
