package com.byconcerts.data.seed

import com.byconcerts.data.local.entity.EventEntity
import kotlinx.serialization.Serializable

/** Formato do seed em assets/events.json. Valores monetários em centavos. */
@Serializable
data class EventSeedDto(
    val id: String,
    val title: String,
    val venue: String,
    val city: String,
    val dateEpochMillis: Long,
    val unitPriceInCents: Long,
    val availableQuantity: Int,
    val imageUrl: String? = null,
    val description: String? = null,
) {
    fun toEntity(): EventEntity = EventEntity(
        id = id,
        title = title,
        venue = venue,
        city = city,
        dateEpochMillis = dateEpochMillis,
        unitPriceInCents = unitPriceInCents,
        availableQuantity = availableQuantity,
        imageUrl = imageUrl,
        description = description,
    )
}
