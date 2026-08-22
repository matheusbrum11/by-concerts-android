package com.byconcerts.feature.checkout.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byconcerts.domain.usecase.ObservePurchaseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ReceiptViewModel(
    private val purchaseId: String,
    private val observePurchase: ObservePurchaseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptState())
    val state: StateFlow<ReceiptState> = _state.asStateFlow()

    init {
        observePurchase(purchaseId)
            .onEach { purchase ->
                _state.value = ReceiptState(
                    isLoading = false,
                    purchase = purchase,
                    errorMessage = if (purchase == null) "Comprovante não encontrado." else null,
                )
            }
            .catch { _state.value = ReceiptState(isLoading = false, errorMessage = "Falha ao carregar o comprovante.") }
            .launchIn(viewModelScope)
    }
}
