package com.byconcerts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val venue: String,
    val city: String,
    val dateEpochMillis: Long,
    val unitPriceInCents: Long,
    val availableQuantity: Int,
    val imageUrl: String?,
    val description: String?,
)
