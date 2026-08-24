package com.byconcerts.data.di

import androidx.room.Room
import com.byconcerts.data.local.db.AppDatabase
import com.byconcerts.data.repository.EventRepositoryImpl
import com.byconcerts.data.repository.PurchaseRepositoryImpl
import com.byconcerts.data.seed.EventSeeder
import com.byconcerts.domain.repository.EventRepository
import com.byconcerts.domain.repository.PurchaseRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME).build()
    }
    single { get<AppDatabase>().eventDao() }
    single { get<AppDatabase>().purchaseDao() }

    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    single<PurchaseRepository> { PurchaseRepositoryImpl(get(), get()) }

    single { EventSeeder(androidContext(), get(), get(), get()) }
}
