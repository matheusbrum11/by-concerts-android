package com.byconcerts.feature.checkout.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.mns.designsystem.component.action.MnsButton
import com.mns.designsystem.component.layout.MnsScaffold
import com.mns.designsystem.component.layout.MnsTopBar
import com.mns.designsystem.component.status.MnsAlert
import com.mns.designsystem.component.code.MnsTicketCard
import com.mns.designsystem.component.text.MnsText
import com.mns.designsystem.format.MnsCurrencyFormatter
import com.mns.designsystem.token.MnsStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReceiptRoute(
    purchaseId: String,
    onDone: () -> Unit,
    viewModel: ReceiptViewModel = koinViewModel(key = purchaseId) { parametersOf(purchaseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReceiptScreen(state = state, onDone = onDone)
}

@Composable
internal fun ReceiptScreen(state: ReceiptState, onDone: () -> Unit) {
    MnsScaffold(
        topBar = { MnsTopBar(title = "Comprovante") },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val purchase = state.purchase
            when {
                state.isLoading -> MnsText(text = "Carregando comprovante…", modifier = Modifier.padding(24.dp))

                state.errorMessage != null ->
                    MnsAlert(message = state.errorMessage, status = MnsStatus.DANGER, modifier = Modifier.padding(16.dp))

                purchase != null && purchase.status == PurchaseStatus.APPROVED ->
                    ApprovedReceipt(purchase = purchase, onDone = onDone)

                purchase != null ->
                    MnsAlert(
                        message = "Compra ${purchase.status.name.lowercase()} — nenhum ingresso emitido.",
                        status = MnsStatus.WARNING,
                        modifier = Modifier.padding(16.dp),
                    )
            }
        }
    }
}

@Composable
private fun ApprovedReceipt(purchase: Purchase, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val payment = purchase.payment
        MnsTicketCard(
            // O QR carrega o id da compra APROVADA (requisito firme do case).
            title = purchase.eventTitle,
            qrContent = purchase.id,
            subtitle = "Ingresso válido — apresente o QR na entrada",
            details = buildList {
                add("Quantidade" to purchase.quantity.toString())
                add("Valor" to MnsCurrencyFormatter.formatCents(purchase.totalInCents))
                if (payment != null) {
                    add("Bandeira" to payment.brand)
                    add("Cartão" to payment.maskedCard)
                    add("NSU" to payment.cieloCode)
                    add("Autorização" to payment.authCode)
                }
                add("Status" to "Aprovado")
            },
            footnote = "Compra ${purchase.id}",
        )
        MnsButton(text = "Concluir", onClick = onDone, fillMaxWidth = true)
    }
}
