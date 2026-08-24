package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.PaymentCallback
import kotlinx.coroutines.CompletableDeferred

/**
 * Ponte entre a [com.byconcerts.payment.PaymentResponseActivity] (que recebe o
 * callback do deeplink) e o gateway suspenso aguardando o resultado.
 *
 * É apenas um atalho para o caminho "app vivo": a fonte de verdade do desfecho
 * é o Room, atualizado por [PaymentCallbackHandler]. Se o processo for morto
 * enquanto o app da Cielo está em primeiro plano, não há ninguém escutando aqui
 * — e o app se recupera lendo a compra conciliada do banco.
 *
 * Como o guard de idempotência garante no máximo uma tentativa PENDING por vez,
 * mantemos um único slot.
 */
class CieloCallbackBus {

    private val lock = Any()
    private var pending: CompletableDeferred<PaymentCallback>? = null

    /** Chamado ANTES de disparar a intent, evitando corrida com um retorno rápido. */
    fun arm(): CompletableDeferred<PaymentCallback> = synchronized(lock) {
        CompletableDeferred<PaymentCallback>().also { pending = it }
    }

    fun publish(callback: PaymentCallback) = synchronized(lock) {
        pending?.complete(callback)
        pending = null
    }

    fun cancel() = synchronized(lock) { pending = null }
}
