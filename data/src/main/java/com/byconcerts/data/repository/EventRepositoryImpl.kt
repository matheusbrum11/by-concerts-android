package com.byconcerts.data.repository

import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.data.local.dao.EventDao
import com.byconcerts.data.mapper.toDomain
import com.byconcerts.domain.model.Event
import com.byconcerts.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val dispatchers: DispatcherProvider,
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        eventDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeEvent(eventId: String): Flow<Event?> =
        eventDao.observeById(eventId).map { it?.toDomain() }

    override suspend fun getEvent(eventId: String): Event? =
        withContext(dispatchers.io) { eventDao.getById(eventId)?.toDomain() }

    override suspend fun decrementAvailability(eventId: String, quantity: Int): Int? =
        withContext(dispatchers.io) {
            eventDao.decrementAvailability(eventId, quantity)
            eventDao.availabilityOf(eventId)
        }
}
