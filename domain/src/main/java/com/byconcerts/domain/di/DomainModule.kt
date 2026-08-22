package com.byconcerts.domain.di

import com.byconcerts.domain.usecase.BuildPaymentRequestUseCase
import com.byconcerts.domain.usecase.CreatePendingPurchaseUseCase
import com.byconcerts.domain.usecase.GetPurchaseUseCase
import com.byconcerts.domain.usecase.ObserveEventUseCase
import com.byconcerts.domain.usecase.ObserveEventsUseCase
import com.byconcerts.domain.usecase.ObservePurchaseUseCase
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Grafo dos use cases. Declarados uma única vez aqui; as features só declaram
 * seus ViewModels (evita definições duplicadas no grafo Koin). */
val domainModule = module {
    factoryOf(::ObserveEventsUseCase)
    factoryOf(::ObserveEventUseCase)
    factoryOf(::ObservePurchaseUseCase)
    factoryOf(::GetPurchaseUseCase)
    factoryOf(::CreatePendingPurchaseUseCase)
    factoryOf(::BuildPaymentRequestUseCase)
    factoryOf(::ReconcilePaymentUseCase)
}
