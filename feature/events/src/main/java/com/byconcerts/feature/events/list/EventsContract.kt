package com.byconcerts.feature.events.list

import com.byconcerts.domain.model.Event

data class EventsState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && events.isEmpty() && errorMessage == null
}

sealed interface EventsIntent {
    data object Retry : EventsIntent
    data class EventClicked(val eventId: String) : EventsIntent
}

sealed interface EventsEffect {
    data class OpenEventDetail(val eventId: String) : EventsEffect
}
