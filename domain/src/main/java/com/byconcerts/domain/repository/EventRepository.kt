package com.byconcerts.domain.repository

import com.byconcerts.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(eventId: String): Flow<Event?>
    suspend fun getEvent(eventId: String): Event?

    suspend fun decrementAvailability(eventId: String, quantity: Int): Int?
}
