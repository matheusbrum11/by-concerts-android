package com.byconcerts.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.byconcerts.data.local.dao.EventDao
import com.byconcerts.data.local.dao.PurchaseDao
import com.byconcerts.data.local.entity.EventEntity
import com.byconcerts.data.local.entity.PurchaseEntity

@Database(
    entities = [EventEntity::class, PurchaseEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        const val NAME = "by_concerts.db"
    }
}
