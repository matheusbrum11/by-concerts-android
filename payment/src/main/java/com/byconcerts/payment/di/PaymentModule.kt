package com.byconcerts.payment.di

import com.byconcerts.payment.BuildConfig
import com.byconcerts.payment.cielo.AndroidDeeplinkLauncher
import com.byconcerts.payment.cielo.CieloCallbackBus
import com.byconcerts.payment.cielo.CieloCredentials
import com.byconcerts.payment.cielo.CieloDeeplinkGateway
import com.byconcerts.payment.cielo.CieloRequestCodec
import com.byconcerts.payment.cielo.CieloResponseParser
import com.byconcerts.payment.cielo.DeeplinkLauncher
import com.byconcerts.payment.cielo.PaymentCallbackHandler
import com.byconcerts.payment.gateway.PaymentGateway
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Grafo Koin do módulo de pagamento. As credenciais vêm de BuildConfig (lidas
 * do local.properties). O barramento de callback é singleton para ser
 * compartilhado entre o gateway (que aguarda) e a PaymentResponseActivity
 * (que publica).
 */
val paymentModule = module {
    single {
        CieloCredentials(
            clientId = BuildConfig.CIELO_CLIENT_ID,
            accessToken = BuildConfig.CIELO_ACCESS_TOKEN,
            merchantCode = BuildConfig.CIELO_MERCHANT_CODE,
        )
    }
    single { CieloCallbackBus() }
    single { CieloResponseParser(get()) }
    single { CieloRequestCodec(get(), get()) }
    single { PaymentCallbackHandler(get(), get(), get()) }
    single<DeeplinkLauncher> { AndroidDeeplinkLauncher(androidContext()) }
    single<PaymentGateway> { CieloDeeplinkGateway(get(), get(), get()) }
}
