package com.byconcerts.tickets.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashKey : NavKey

@Serializable
data object EventsKey : NavKey

@Serializable
data class EventDetailKey(val eventId: String) : NavKey

@Serializable
data class CheckoutKey(val eventId: String, val quantity: Int) : NavKey

@Serializable
data class ReceiptKey(val purchaseId: String) : NavKey
