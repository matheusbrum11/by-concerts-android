package com.byconcerts.tickets.di

import com.byconcerts.core.common.di.coreModule
import com.byconcerts.data.di.dataModule
import com.byconcerts.domain.di.domainModule
import com.byconcerts.feature.checkout.di.checkoutModule
import com.byconcerts.feature.events.di.eventsModule
import com.byconcerts.payment.di.paymentModule
import com.byconcerts.tickets.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appModule: Module = module {
    viewModel { SplashViewModel(get(), get()) }
}

val appModules: List<Module> = listOf(
    coreModule,
    dataModule,
    domainModule,
    paymentModule,
    eventsModule,
    checkoutModule,
    appModule,
)
