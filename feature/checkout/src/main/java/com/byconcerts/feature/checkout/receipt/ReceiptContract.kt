package com.byconcerts.feature.checkout.receipt

import com.byconcerts.domain.model.Purchase

data class ReceiptState(
    val isLoading: Boolean = true,
    val purchase: Purchase? = null,
    val errorMessage: String? = null,
)
