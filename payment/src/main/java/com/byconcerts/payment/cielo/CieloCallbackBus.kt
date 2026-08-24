package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.PaymentCallback
import kotlinx.coroutines.CompletableDeferred

class CieloCallbackBus {

    private val lock = Any()
    private var pending: CompletableDeferred<PaymentCallback>? = null

    fun arm(): CompletableDeferred<PaymentCallback> = synchronized(lock) {
        CompletableDeferred<PaymentCallback>().also { pending = it }
    }

    fun publish(callback: PaymentCallback) = synchronized(lock) {
        pending?.complete(callback)
        pending = null
    }

    fun cancel() = synchronized(lock) { pending = null }
}
