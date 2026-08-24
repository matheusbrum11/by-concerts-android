package com.byconcerts.payment.cielo

import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.payment.gateway.PaymentGateway
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementação principal do [PaymentGateway]: integração Cielo via DEEPLINK,
 * conforme o sample oficial da Cielo.
 *
 * O app NÃO embarca o SDK da Cielo — apenas dispara uma Intent
 * (`lio://payment`) e aguarda o callback (`order://payment`), por isso não
 * herda a restrição de targetSdk 29 do modelo de SDK embarcado.
 *
 * Fluxo: verifica disponibilidade → arma o barramento → dispara a intent →
 * suspende até o callback (com timeout). Falha de disparo (app da Cielo
 * ausente) e timeout viram [PaymentError] explícitos.
 *
 * Importante: este `await` é só o caminho "app vivo". A persistência do
 * desfecho é feita por [PaymentCallbackHandler] no momento do callback, de modo
 * que um timeout aqui NÃO significa que o pagamento não ocorreu — a compra
 * segue PENDING e é conciliada pelo `reference` assim que o retorno chega.
 */
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
        const val DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L // 5 min
    }
}
