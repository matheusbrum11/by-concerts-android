package com.byconcerts.domain.usecase

import com.byconcerts.domain.model.Event
import com.byconcerts.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

/** Fluxo reativo da lista de eventos disponíveis. */
class ObserveEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.observeEvents()
}
