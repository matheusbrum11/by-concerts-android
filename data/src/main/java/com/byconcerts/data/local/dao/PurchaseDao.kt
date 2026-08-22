package com.byconcerts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.byconcerts.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    /**
     * Insere IGNORANDO conflitos. Se a chave de idempotência já existe, a linha
     * antiga é preservada e o retorno é -1 — o repositório detecta isso e devolve
     * a compra existente em vez de criar outra.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringConflict(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases WHERE idempotencyKey = :key")
    suspend fun findByIdempotencyKey(key: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    fun observeById(id: String): Flow<PurchaseEntity?>

    @Update
    suspend fun update(purchase: PurchaseEntity)
}
