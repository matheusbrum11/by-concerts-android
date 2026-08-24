package com.byconcerts.domain.usecase

import com.byconcerts.core.common.AppResult
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.error.DomainResult
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.repository.PurchaseRepository

class GetPurchaseUseCase(private val repository: PurchaseRepository) {
    suspend operator fun invoke(purchaseId: String): DomainResult<Purchase> {
        val purchase = repository.getById(purchaseId)
            ?: return AppResult.Failure(DomainError.PurchaseNotFound)
        return AppResult.Success(purchase)
    }
}
