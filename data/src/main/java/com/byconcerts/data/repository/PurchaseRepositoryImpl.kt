package com.byconcerts.data.repository

import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.data.local.dao.PurchaseDao
import com.byconcerts.data.mapper.toDomain
import com.byconcerts.data.mapper.toEntity
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PurchaseRepositoryImpl(
    private val purchaseDao: PurchaseDao,
    private val dispatchers: DispatcherProvider,
) : PurchaseRepository {

    override suspend fun createPendingIfAbsent(purchase: Purchase): Purchase =
        withContext(dispatchers.io) {
            val rowId = purchaseDao.insertIgnoringConflict(purchase.toEntity())
            if (rowId == -1L) {
                // Conflito de chave de idempotência: já havia uma compra. Devolve
                // a existente em vez de criar outra (não-duplicação).
                purchaseDao.findByIdempotencyKey(purchase.idempotencyKey)?.toDomain() ?: purchase
            } else {
                purchase
            }
        }

    override suspend fun findByIdempotencyKey(idempotencyKey: String): Purchase? =
        withContext(dispatchers.io) {
            purchaseDao.findByIdempotencyKey(idempotencyKey)?.toDomain()
        }

    override suspend fun getById(purchaseId: String): Purchase? =
        withContext(dispatchers.io) { purchaseDao.getById(purchaseId)?.toDomain() }

    override fun observePurchase(purchaseId: String): Flow<Purchase?> =
        purchaseDao.observeById(purchaseId).map { it?.toDomain() }

    override suspend fun update(purchase: Purchase) =
        withContext(dispatchers.io) { purchaseDao.update(purchase.toEntity()) }
}
