package com.byconcerts.domain.repository

import com.byconcerts.domain.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Fonte de eventos. A implementação atual é Room (seed local), mas trocar por
 * uma API remota seria apenas uma nova implementação desta interface.
 */
interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(eventId: String): Flow<Event?>
    suspend fun getEvent(eventId: String): Event?

    /**
     * Baixa o estoque do evento após uma compra aprovada. Retorna a quantidade
     * disponível resultante, ou null se o evento não existe.
     */
    suspend fun decrementAvailability(eventId: String, quantity: Int): Int?
}
