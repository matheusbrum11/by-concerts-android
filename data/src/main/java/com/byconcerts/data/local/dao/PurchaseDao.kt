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
