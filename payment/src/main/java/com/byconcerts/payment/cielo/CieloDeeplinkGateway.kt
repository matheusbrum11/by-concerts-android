package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.payment.gateway.PaymentGateway
import kotlinx.coroutines.withTimeoutOrNull

class CieloDeeplinkGateway(
    private val codec: CieloRequestCodec,
    private val launcher: DeeplinkLauncher,
    private val callbackBus: CieloCallbackBus,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : PaymentGateway {

    override suspend fun pay(request: PaymentRequest): PaymentResult =
        dispatch(codec.buildCheckoutUri(request))

    override suspend fun cancel(request: CancellationRequest): PaymentResult =
        dispatch(codec.buildReversalUri(request))

    private suspend fun dispatch(uri: String): PaymentResult {
        if (!launcher.isPaymentAppAvailable()) {
            return PaymentResult.Error(PaymentError.GatewayNotAvailable)
        }

        val deferred = callbackBus.arm()
        if (!launcher.launch(uri)) {
            callbackBus.cancel()
            return PaymentResult.Error(PaymentError.GatewayNotAvailable)
        }

        val callback = withTimeoutOrNull(timeoutMillis) { deferred.await() }
        if (callback == null) {
            callbackBus.cancel()
            return PaymentResult.Error(PaymentError.Timeout)
        }
        return callback.result
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L
    }
}
