package com.byconcerts.tickets

import android.app.Application
import com.byconcerts.tickets.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ByConcertsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ByConcertsApp)
            modules(appModules)
        }
    }
}
