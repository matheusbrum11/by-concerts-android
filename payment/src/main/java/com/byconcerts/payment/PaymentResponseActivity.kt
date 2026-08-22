package com.byconcerts.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.byconcerts.payment.cielo.CieloCallbackBus
import com.byconcerts.payment.cielo.CieloResponseParser
import org.koin.android.ext.android.inject

/**
 * Activity que recebe o callback do deeplink da Cielo (order://response). Faz o
 * parse do parâmetro `response`, publica o [com.byconcerts.domain.model.PaymentResult]
 * no barramento (retomando o gateway suspenso) e finaliza, devolvendo o usuário
 * à tela de checkout.
 *
 * Declarada como exported com intent-filter (host=response, scheme=order) —
 * ver AndroidManifest do módulo.
 */
class PaymentResponseActivity : Activity() {

    private val parser: CieloResponseParser by inject()
    private val callbackBus: CieloCallbackBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCallback(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallback(intent)
        finish()
    }

    private fun handleCallback(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val response = intent.data?.getQueryParameter("response")
        callbackBus.publish(parser.parse(response))
    }
}
