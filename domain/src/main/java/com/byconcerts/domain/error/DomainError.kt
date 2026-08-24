package com.byconcerts.domain.error

import com.byconcerts.core.common.AppResult
import com.byconcerts.domain.model.PaymentError

sealed interface DomainError {
    data class EventNotFound(val eventId: String) : DomainError

    data class OutOfStock(val eventId: String, val requested: Int, val available: Int) : DomainError

    data object InvalidQuantity : DomainError

    data object PurchaseNotFound : DomainError

    data class Storage(val message: String? = null) : DomainError

    data class Payment(val error: PaymentError) : DomainError
}

typealias DomainResult<D> = AppResult<D, DomainError>
