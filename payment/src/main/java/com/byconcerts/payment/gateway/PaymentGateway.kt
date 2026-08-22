package com.byconcerts.payment.gateway

import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult

/**
 * Abstração de pagamento. O resto do app depende SÓ desta interface — a
 * implementação Cielo (Deeplink) fica isolada atrás dela. Uma futura
 * implementação via SDK legado seria apenas outra classe que a implementa
 * (ver README, trade-offs).
 */
interface PaymentGateway {
    suspend fun pay(request: PaymentRequest): PaymentResult
    suspend fun cancel(request: CancellationRequest): PaymentResult
}
