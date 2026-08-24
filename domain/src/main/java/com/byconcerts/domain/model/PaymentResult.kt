package com.byconcerts.domain.model

sealed interface PaymentResult {
    data class Approved(val payment: PaymentInfo) : PaymentResult

    data class Denied(val reason: String, val code: Int? = null) : PaymentResult

    data object Canceled : PaymentResult

    data class Error(val type: PaymentError) : PaymentResult
}

sealed interface PaymentError {
    data object GatewayNotAvailable : PaymentError

    data object InvalidResponse : PaymentError

    data class PartialPayment(val pendingInCents: Long) : PaymentError

    data object Timeout : PaymentError

    data class Unknown(val message: String? = null) : PaymentError
}
