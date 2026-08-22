package com.byconcerts.feature.events.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byconcerts.domain.model.Event
import com.byconcerts.feature.events.util.toEventDateLabel
import com.mns.designsystem.component.layout.MnsCard
import com.mns.designsystem.component.layout.MnsScaffold
import com.mns.designsystem.component.layout.MnsTopBar
import com.mns.designsystem.component.status.MnsAlert
import com.mns.designsystem.component.status.MnsEmptyState
import com.mns.designsystem.component.status.MnsTag
import com.mns.designsystem.component.text.MnsHeading
import com.mns.designsystem.component.text.MnsText
import com.mns.designsystem.format.MnsCurrencyFormatter
import com.mns.designsystem.theme.MnsTheme
import com.mns.designsystem.token.MnsStatus
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun EventsListRoute(
    onOpenEvent: (String) -> Unit,
    viewModel: EventsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is EventsEffect.OpenEventDetail -> onOpenEvent(effect.eventId)
            }
        }
    }
    EventsListScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun EventsListScreen(
    state: EventsState,
    onIntent: (EventsIntent) -> Unit,
) {
    MnsScaffold(
        topBar = { MnsTopBar(title = "Eventos", subtitle = "Ingressos para eventos locais") },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> MnsText(text = "Carregando eventos…")

                state.errorMessage != null -> MnsAlert(
                    message = state.errorMessage,
                    status = MnsStatus.DANGER,
                    modifier = Modifier.padding(16.dp),
                )

                state.isEmpty -> MnsEmptyState(
                    title = "Nenhum evento disponível",
                    description = "Volte mais tarde para conferir novos eventos.",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventRow(
                            event = event,
                            onClick = { onIntent(EventsIntent.EventClicked(event.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: Event, onClick: () -> Unit) {
    MnsCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        MnsHeading(
            text = event.title,
            level = com.mns.designsystem.component.text.MnsHeadingLevel.H3,
            overline = event.dateEpochMillis.toEventDateLabel(),
            subtitle = "${event.venue} · ${event.city}",
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MnsText(
                text = MnsCurrencyFormatter.formatCents(event.unitPriceInCents),
                style = MnsTheme.typography.titleMedium,
            )
            if (event.isSoldOut) {
                MnsTag(text = "Esgotado", status = MnsStatus.DANGER)
            } else {
                MnsTag(text = "${event.availableQuantity} disponíveis", status = MnsStatus.SUCCESS)
            }
        }
    }
}
