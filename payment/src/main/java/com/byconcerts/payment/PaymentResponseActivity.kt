package com.byconcerts.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.byconcerts.payment.cielo.PaymentCallbackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Recebe o callback do deeplink da Cielo (`order://payment`), declarada no
 * manifest com `<data android:scheme="order" android:host="payment"/>`.
 *
 * Não tem UI: delega ao [PaymentCallbackHandler] (parse → concilia/persiste →
 * publica) e finaliza, devolvendo o usuário à tela de checkout.
 *
 * A conciliação roda em um escopo de APLICAÇÃO, não no da Activity: o `finish()`
 * é imediato e o trabalho de persistir o desfecho não pode ser cancelado junto
 * — é justamente ele que garante que a compra não fique órfã em PENDING.
 */
class PaymentResponseActivity : Activity() {

    private val callbackHandler: PaymentCallbackHandler by inject()
    private val appScope: CoroutineScope by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCallback(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallback(intent)
        finish()
    }

    private fun handleCallback(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        appScope.launch { callbackHandler.handle(data) }
    }
}
