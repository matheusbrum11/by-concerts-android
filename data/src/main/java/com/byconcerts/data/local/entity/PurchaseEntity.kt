package com.byconcerts.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchases",
    indices = [Index(value = ["idempotencyKey"], unique = true)],
)
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val totalInCents: Long,
    val status: String,
    val paymentCode: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val authCode: String?,
    val cieloCode: String?,
    val brand: String?,
    val maskedCard: String?,
    val amountInCents: Long?,
    val failureReason: String?,
)
