package com.byconcerts.domain.repository

import com.byconcerts.domain.model.Purchase
import kotlinx.coroutines.flow.Flow

/**
 * Persistência de compras. É aqui que a idempotência se materializa: a inserção
 * de PENDING respeita a constraint UNIQUE da chave de idempotência.
 */
interface PurchaseRepository {

    /**
     * Insere a compra PENDING. Se JÁ existir uma compra com a mesma
     * [Purchase.idempotencyKey], NÃO cria outra e devolve a existente — é a
     * primeira barreira contra duplo disparo.
     */
    suspend fun createPendingIfAbsent(purchase: Purchase): Purchase

    suspend fun findByIdempotencyKey(idempotencyKey: String): Purchase?
    suspend fun getById(purchaseId: String): Purchase?
    fun observePurchase(purchaseId: String): Flow<Purchase?>

    /** Atualiza (upsert) o estado/dados da compra após a conciliação. */
    suspend fun update(purchase: Purchase)
}
