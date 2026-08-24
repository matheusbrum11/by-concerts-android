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

/**
 * Singletons fundamentais compartilhados por todas as camadas. Fica aqui (e não
 * espalhado) para evitar definições duplicadas de tipos como [Json] no grafo.
 */
val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { SystemClock() }
    single<IdGenerator> { UuidGenerator() }
    single {
        Json {
            ignoreUnknownKeys = true
            // A Cielo devolve campos como string vazia/valores não declarados;
            // ser leniente evita quebrar o parse do callback por um campo novo.
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    /**
     * Escopo de aplicação para trabalho que não pode morrer junto com uma tela
     * — em especial a conciliação do callback de pagamento, disparada por uma
     * Activity que finaliza imediatamente.
     */
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io) }
}
