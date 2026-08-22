package com.byconcerts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.byconcerts.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY dateEpochMillis ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>)

    /**
     * Baixa condicional: só decrementa se houver estoque suficiente. Retorna o
     * número de linhas afetadas (0 = não havia estoque / evento inexistente).
     */
    @Query(
        "UPDATE events SET availableQuantity = availableQuantity - :quantity " +
            "WHERE id = :id AND availableQuantity >= :quantity",
    )
    suspend fun decrementAvailability(id: String, quantity: Int): Int

    @Query("SELECT availableQuantity FROM events WHERE id = :id")
    suspend fun availabilityOf(id: String): Int?
}
