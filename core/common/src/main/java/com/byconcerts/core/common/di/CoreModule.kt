package com.byconcerts.core.common.di

import com.byconcerts.core.common.Clock
import com.byconcerts.core.common.DefaultDispatcherProvider
import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.core.common.IdGenerator
import com.byconcerts.core.common.SystemClock
import com.byconcerts.core.common.UuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { SystemClock() }
    single<IdGenerator> { UuidGenerator() }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    single<CoroutineScope> { CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io) }
}
