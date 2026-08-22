package com.byconcerts.data.mapper

import com.byconcerts.data.local.entity.EventEntity
import com.byconcerts.domain.model.Event

fun EventEntity.toDomain(): Event = Event(
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

fun Event.toEntity(): EventEntity = EventEntity(
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
