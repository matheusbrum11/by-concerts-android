package com.byconcerts.domain.usecase

import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow

/** Fluxo reativo de uma compra (tela de comprovante/estado do pagamento). */
class ObservePurchaseUseCase(private val repository: PurchaseRepository) {
    operator fun invoke(purchaseId: String): Flow<Purchase?> =
        repository.observePurchase(purchaseId)
}
