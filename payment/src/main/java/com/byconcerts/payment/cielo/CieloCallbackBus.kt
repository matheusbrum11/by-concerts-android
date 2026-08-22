package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.PaymentResult
import kotlinx.coroutines.CompletableDeferred

/**
 * Ponte entre a [com.byconcerts.payment.PaymentResponseActivity] (que recebe o
 * callback do deeplink) e o gateway suspenso aguardando o resultado.
 *
 * Como o guard de idempotência garante NO MÁXIMO uma tentativa PENDING por vez,
 * mantemos um único slot. `arm()` é chamado ANTES de disparar a intent (evita
 * corrida com um callback muito rápido); a Activity chama `publish()`.
 */
class CieloCallbackBus {

    private val lock = Any()
    private var pending: CompletableDeferred<PaymentResult>? = null

    fun arm(): CompletableDeferred<PaymentResult> = synchronized(lock) {
        CompletableDeferred<PaymentResult>().also { pending = it }
    }

    fun publish(result: PaymentResult) = synchronized(lock) {
        pending?.complete(result)
        pending = null
    }
}
