package com.byconcerts.feature.events.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byconcerts.feature.events.util.toEventDateLabel
import com.mns.designsystem.component.action.MnsButton
import com.mns.designsystem.component.input.MnsStepper
import com.mns.designsystem.component.layout.MnsScaffold
import com.mns.designsystem.component.layout.MnsTopBar
import com.mns.designsystem.component.media.MnsCover
import com.mns.designsystem.component.status.MnsAlert
import com.mns.designsystem.component.text.MnsHeading
import com.mns.designsystem.component.text.MnsHeadingLevel
import com.mns.designsystem.component.text.MnsText
import com.mns.designsystem.format.MnsCurrencyFormatter
import com.mns.designsystem.theme.MnsTheme
import com.mns.designsystem.token.MnsStatus
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EventDetailRoute(
    eventId: String,
    onBack: () -> Unit,
    onOpenCheckout: (eventId: String, quantity: Int) -> Unit,
    viewModel: EventDetailViewModel = koinViewModel(key = eventId) { parametersOf(eventId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is EventDetailEffect.OpenCheckout -> onOpenCheckout(effect.eventId, effect.quantity)
            }
        }
    }
    EventDetailScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
internal fun EventDetailScreen(
    state: EventDetailState,
    onIntent: (EventDetailIntent) -> Unit,
    onBack: () -> Unit,
) {
    MnsScaffold(
        topBar = { MnsTopBar(title = "Detalhe", onNavigateBack = onBack) },
        bottomBar = {
            if (state.event != null) {
                BuyBar(
                    totalLabel = MnsCurrencyFormatter.formatCents(state.totalInCents),
                    enabled = state.canBuy,
                    onBuy = { onIntent(EventDetailIntent.BuyClicked) },
                )
            }
        },
    ) { padding ->
        val event = state.event
        when {
            state.errorMessage != null -> MnsAlert(
                message = state.errorMessage,
                status = MnsStatus.DANGER,
                modifier = Modifier.padding(16.dp),
            )

            event != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MnsCover(painter = null, contentDescription = event.title)
                MnsHeading(
                    text = event.title,
                    level = MnsHeadingLevel.H1,
                    overline = event.dateEpochMillis.toEventDateLabel(),
                    subtitle = "${event.venue} · ${event.city}",
                )
                event.description?.let { MnsText(text = it, style = MnsTheme.typography.bodyMedium) }

                MnsText(
                    text = "Valor unitário: ${MnsCurrencyFormatter.formatCents(event.unitPriceInCents)}",
                    style = MnsTheme.typography.titleSmall,
                )

                if (event.isSoldOut) {
                    MnsAlert(message = "Ingressos esgotados para este evento.", status = MnsStatus.WARNING)
                } else {
                    MnsStepper(
                        value = state.quantity,
                        onValueChange = { onIntent(EventDetailIntent.QuantityChanged(it)) },
                        range = 1..state.maxQuantity,
                        label = "Quantidade",
                    )
                }
            }
        }
    }
}

@Composable
private fun BuyBar(totalLabel: String, enabled: Boolean, onBuy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            MnsText(text = "Total", style = MnsTheme.typography.labelMedium)
            MnsText(text = totalLabel, style = MnsTheme.typography.titleLarge)
        }
        MnsButton(text = "Comprar", onClick = onBuy, enabled = enabled)
    }
}
