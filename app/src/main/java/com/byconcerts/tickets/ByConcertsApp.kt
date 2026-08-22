package com.byconcerts.tickets

import android.app.Application
import com.byconcerts.data.seed.EventSeeder
import com.byconcerts.tickets.di.appModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application do app. Inicializa o grafo Koin e popula o Room a partir do seed
 * (assets/events.json) no primeiro boot — mantém o app executável sem backend
 * nem credenciais externas.
 */
class ByConcertsApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventSeeder: EventSeeder by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ByConcertsApp)
            modules(appModules)
        }
        appScope.launch { eventSeeder.seedIfEmpty() }
    }
}
