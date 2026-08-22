package com.byconcerts.core.common.di

import com.byconcerts.core.common.Clock
import com.byconcerts.core.common.DefaultDispatcherProvider
import com.byconcerts.core.common.DispatcherProvider
import com.byconcerts.core.common.IdGenerator
import com.byconcerts.core.common.SystemClock
import com.byconcerts.core.common.UuidGenerator
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Singletons fundamentais compartilhados por todas as camadas. Fica aqui (e não
 * espalhado) para evitar definições duplicadas de tipos como [Json] no grafo.
 */
val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { SystemClock() }
    single<IdGenerator> { UuidGenerator() }
    single { Json { ignoreUnknownKeys = true } }
}
