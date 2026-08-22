package com.byconcerts.tickets.di

import com.byconcerts.core.common.di.coreModule
import com.byconcerts.data.di.dataModule
import com.byconcerts.domain.di.domainModule
import com.byconcerts.feature.checkout.di.checkoutModule
import com.byconcerts.feature.events.di.eventsModule
import com.byconcerts.payment.di.paymentModule
import org.koin.core.module.Module

/** Agregador dos módulos Koin de cada camada. */
val appModules: List<Module> = listOf(
    coreModule,
    dataModule,
    domainModule,
    paymentModule,
    eventsModule,
    checkoutModule,
)
