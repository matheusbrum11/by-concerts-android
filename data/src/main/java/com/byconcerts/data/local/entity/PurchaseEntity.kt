package com.byconcerts.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Compra persistida. A constraint UNIQUE em [idempotencyKey] é a garantia final,
 * a nível de banco, contra cobrança duplicada: duas inserções com a mesma chave
 * não coexistem. Campos de pagamento são nulos até a aprovação.
 */
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
    // ── PaymentInfo (presente quando APPROVED) ──
    val authCode: String?,
    val cieloCode: String?,
    val brand: String?,
    val maskedCard: String?,
    val amountInCents: Long?,
    val failureReason: String?,
)
