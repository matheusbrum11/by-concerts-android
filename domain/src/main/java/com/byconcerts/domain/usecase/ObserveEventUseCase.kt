package com.byconcerts.domain.usecase

import com.byconcerts.domain.model.Event
import com.byconcerts.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

class ObserveEventUseCase(private val repository: EventRepository) {
    operator fun invoke(eventId: String): Flow<Event?> = repository.observeEvent(eventId)
}
