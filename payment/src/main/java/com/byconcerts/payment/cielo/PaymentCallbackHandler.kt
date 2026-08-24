package com.byconcerts.payment.cielo

import android.net.Uri
import com.byconcerts.domain.model.PaymentCallback
import com.byconcerts.domain.usecase.ReconcilePaymentUseCase

class PaymentCallbackHandler(
    private val parser: CieloResponseParser,
    private val reconcilePayment: ReconcilePaymentUseCase,
    private val callbackBus: CieloCallbackBus,
) {
    suspend fun handle(uri: Uri?): PaymentCallback {
        val callback = parser.parse(uri)

        callback.reference?.let { reference ->
            reconcilePayment(reference, callback.result)
        }

        callbackBus.publish(callback)
        return callback
    }
}
