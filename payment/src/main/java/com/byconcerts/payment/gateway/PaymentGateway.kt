package com.byconcerts.payment.gateway

import com.byconcerts.domain.model.CancellationRequest
import com.byconcerts.domain.model.PaymentRequest
import com.byconcerts.domain.model.PaymentResult

interface PaymentGateway {
    suspend fun pay(request: PaymentRequest): PaymentResult
    suspend fun cancel(request: CancellationRequest): PaymentResult
}
