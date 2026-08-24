package com.byconcerts.feature.events.detail

import androidx.lifecycle.viewModelScope
import com.byconcerts.core.ui.mvi.MviViewModel
import com.byconcerts.domain.usecase.ObserveEventUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class EventDetailViewModel(
    private val eventId: String,
    private val observeEvent: ObserveEventUseCase,
) : MviViewModel<EventDetailState, EventDetailIntent, EventDetailEffect>(EventDetailState()) {

    init {
        observeEvent(eventId)
            .onEach { event ->
                setState {
                    copy(
                        isLoading = false,
                        event = event,
                        quantity = quantity.coerceIn(1, (event?.availableQuantity ?: 1).coerceAtLeast(1)),
                        errorMessage = if (event == null) "Evento não encontrado." else null,
                    )
                }
            }
            .catch { setState { copy(isLoading = false, errorMessage = "Falha ao carregar o evento.") } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: EventDetailIntent) {
        when (intent) {
            is EventDetailIntent.QuantityChanged -> setState { copy(quantity = intent.quantity) }
            EventDetailIntent.BuyClicked -> {
                val state = currentState
                if (state.canBuy && state.event != null) {
                    sendEffect(EventDetailEffect.OpenCheckout(state.event.id, state.quantity))
                }
            }
        }
    }
}
