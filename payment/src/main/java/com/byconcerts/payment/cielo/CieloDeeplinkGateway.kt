package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.payment.gateway.PaymentGateway
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementação principal do [PaymentGateway]: integração Cielo via DEEPLINK.
 * O app NÃO embarca o SDK da Cielo — apenas dispara uma Intent e aguarda o
 * callback, por isso não herda a restrição de targetSdk 29 do SDK embarcado.
 *
 * Fluxo: monta a URI → arma o barramento de callback → dispara a intent →
 * suspende até o callback (com timeout). Falha de disparo (app Cielo ausente)
 * e timeout viram [PaymentError] explícitos.
 */
class CieloDeeplinkGateway(
    private val codec: CieloRequestCodec,
    private val launcher: DeeplinkLauncher,
    private val callbackBus: CieloCallbackBus,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : PaymentGateway {

    override suspend fun pay(request: PaymentRequest): PaymentResult {
        val deferred = callbackBus.arm()
        val launched = launcher.launch(codec.buildCheckoutUri(request))
        if (!launched) {
            callbackBus.publish(PaymentResult.Error(PaymentError.GatewayNotAvailable))
            return PaymentResult.Error(PaymentError.GatewayNotAvailable)
        }
        return withTimeoutOrNull(timeoutMillis) { deferred.await() }
            ?: PaymentResult.Error(PaymentError.Timeout)
    }

    override suspend fun cancel(request: CancellationRequest): PaymentResult {
        val deferred = callbackBus.arm()
        val launched = launcher.launch(codec.buildReversalUri(request))
        if (!launched) {
            callbackBus.publish(PaymentResult.Error(PaymentError.GatewayNotAvailable))
            return PaymentResult.Error(PaymentError.GatewayNotAvailable)
        }
        return withTimeoutOrNull(timeoutMillis) { deferred.await() }
            ?: PaymentResult.Error(PaymentError.Timeout)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L // 5 min
    }
}
