package com.byconcerts.data.seed

import android.content.Context
import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.data.local.dao.EventDao
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Popula o Room a partir de assets/events.json no primeiro boot (tabela vazia).
 * Mantém o projeto executável sem backend nem credenciais — atrito zero para o
 * avaliador rodar.
 */
class EventSeeder(
    private val context: Context,
    private val eventDao: EventDao,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) {
    suspend fun seedIfEmpty() = withContext(dispatchers.io) {
        if (eventDao.count() > 0) return@withContext
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        val events = json.decodeFromString<List<EventSeedDto>>(raw)
        eventDao.upsertAll(events.map { it.toEntity() })
    }

    private companion object {
        const val ASSET_FILE = "events.json"
    }
}
