package com.byconcerts.feature.events.list

import com.byconcerts.domain.model.Event

/** State único e imutável da listagem de eventos. */
data class EventsState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && events.isEmpty() && errorMessage == null
}

/** Ações do usuário na listagem. */
sealed interface EventsIntent {
    data object Retry : EventsIntent
    data class EventClicked(val eventId: String) : EventsIntent
}

/** Eventos one-shot (navegação). */
sealed interface EventsEffect {
    data class OpenEventDetail(val eventId: String) : EventsEffect
}
