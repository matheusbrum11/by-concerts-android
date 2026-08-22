package com.byconcerts.domain.usecase

import com.byconcerts.domain.model.Event
import com.byconcerts.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

/** Fluxo reativo de um evento específico (tela de detalhe/seleção). */
class ObserveEventUseCase(private val repository: EventRepository) {
    operator fun invoke(eventId: String): Flow<Event?> = repository.observeEvent(eventId)
}
