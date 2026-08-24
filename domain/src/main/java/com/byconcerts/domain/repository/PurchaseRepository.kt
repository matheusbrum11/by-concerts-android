package com.byconcerts.domain.repository

import com.byconcerts.domain.model.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {

    suspend fun createPendingIfAbsent(purchase: Purchase): Purchase

    suspend fun findByIdempotencyKey(idempotencyKey: String): Purchase?
    suspend fun getById(purchaseId: String): Purchase?
    fun observePurchase(purchaseId: String): Flow<Purchase?>

    suspend fun update(purchase: Purchase)
}
