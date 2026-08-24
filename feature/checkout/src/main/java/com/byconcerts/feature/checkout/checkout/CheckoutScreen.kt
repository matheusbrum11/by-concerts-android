package com.byconcerts.feature.checkout.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byconcerts.domain.model.PaymentCode
import com.mns.designsystem.component.action.MnsButton
import com.mns.designsystem.component.action.MnsButtonVariant
import com.mns.designsystem.component.layout.MnsCard
import com.mns.designsystem.component.layout.MnsScaffold
import com.mns.designsystem.component.layout.MnsTopBar
import com.mns.designsystem.component.status.MnsAlert
import com.mns.designsystem.component.text.MnsText
import com.mns.designsystem.format.MnsCurrencyFormatter
import com.mns.designsystem.theme.MnsTheme
import com.mns.designsystem.token.MnsStatus
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CheckoutRoute(
    eventId: String,
    quantity: Int,
    onBack: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(key = "$eventId/$quantity") { parametersOf(eventId, quantity) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is CheckoutEffect.OpenReceipt -> onOpenReceipt(effect.purchaseId)
                is CheckoutEffect.ShowMessage -> Unit
            }
        }
    }
    CheckoutScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
internal fun CheckoutScreen(
    state: CheckoutState,
    onIntent: (CheckoutIntent) -> Unit,
    onBack: () -> Unit,
) {
    MnsScaffold(
        topBar = { MnsTopBar(title = "Pagamento", onNavigateBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val event = state.event
            if (event != null) {
                MnsCard(modifier = Modifier.fillMaxWidth()) {
                    MnsText(text = event.title, style = MnsTheme.typography.titleMedium)
                    SummaryRow("Quantidade", state.quantity.toString())
                    SummaryRow("Valor unitário", MnsCurrencyFormatter.formatCents(event.unitPriceInCents))
                    SummaryRow("Total", MnsCurrencyFormatter.formatCents(state.totalInCents))
                }

                MnsText(text = "Forma de pagamento", style = MnsTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentCode.selectableInCheckout.forEach { code ->
                        PaymentOption(
                            label = code.label(),
                            selected = state.paymentCode == code,
                            enabled = state.isPayEnabled,
                            onClick = { onIntent(CheckoutIntent.PaymentCodeChanged(code)) },
                        )
                    }
                }

                StatusArea(state.phase)

                MnsButton(
                    text = "Pagar ${MnsCurrencyFormatter.formatCents(state.totalInCents)}",
                    onClick = { onIntent(CheckoutIntent.PayClicked) },
                    enabled = state.isPayEnabled,
                    loading = state.phase == CheckoutPhase.Processing,
                    fillMaxWidth = true,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MnsText(text = label, style = MnsTheme.typography.bodyMedium)
        MnsText(text = value, style = MnsTheme.typography.bodyMedium)
    }
}

private fun PaymentCode.label(): String = when (this) {
    PaymentCode.CREDITO_AVISTA -> "Crédito"
    PaymentCode.DEBITO_AVISTA -> "Débito"
    PaymentCode.PIX -> "Pix"
    else -> wireValue
}

@Composable
private fun PaymentOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    MnsButton(
        text = label,
        onClick = onClick,
        enabled = enabled,
        variant = if (selected) MnsButtonVariant.PRIMARY else MnsButtonVariant.OUTLINED,
    )
}

@Composable
private fun StatusArea(phase: CheckoutPhase) {
    when (phase) {
        CheckoutPhase.Processing -> MnsAlert(
            message = "Aguardando confirmação do pagamento no app da Cielo…",
            status = MnsStatus.INFO,
        )

        is CheckoutPhase.Failed -> MnsAlert(message = phase.message, status = MnsStatus.DANGER)
        is CheckoutPhase.PendingRetry -> MnsAlert(message = phase.message, status = MnsStatus.WARNING)
        CheckoutPhase.Approved -> MnsAlert(message = "Pagamento aprovado!", status = MnsStatus.SUCCESS)
        CheckoutPhase.Idle -> Unit
    }
}
