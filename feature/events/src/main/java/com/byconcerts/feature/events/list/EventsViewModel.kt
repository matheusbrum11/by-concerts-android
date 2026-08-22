package com.byconcerts.feature.events.list

import androidx.lifecycle.viewModelScope
import com.byconcerts.core.ui.mvi.MviViewModel
import com.byconcerts.domain.usecase.ObserveEventsUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EventsViewModel(
    private val observeEvents: ObserveEventsUseCase,
) : MviViewModel<EventsState, EventsIntent, EventsEffect>(EventsState()) {

    init {
        observe()
    }

    override fun onIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.Retry -> observe()
            is EventsIntent.EventClicked -> sendEffect(EventsEffect.OpenEventDetail(intent.eventId))
        }
    }

    private fun observe() {
        setState { copy(isLoading = true, errorMessage = null) }
        observeEvents()
            .onEach { events -> setState { copy(isLoading = false, events = events, errorMessage = null) } }
            .catch { setState { copy(isLoading = false, errorMessage = "Não foi possível carregar os eventos.") } }
            .launchIn(viewModelScope)
    }
}
