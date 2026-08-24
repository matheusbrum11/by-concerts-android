package com.byconcerts.feature.events

import app.cash.turbine.test
import com.byconcerts.domain.model.Event
import com.byconcerts.domain.usecase.ObserveEventsUseCase
import com.byconcerts.feature.events.list.EventsEffect
import com.byconcerts.feature.events.list.EventsIntent
import com.byconcerts.feature.events.list.EventsViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeEvents = mockk<ObserveEventsUseCase>()

    private val events = listOf(
        Event("evt-1", "Show A", "Arena", "SP", 1L, 5000, 10, null),
        Event("evt-2", "Show B", "Casa", "RJ", 2L, 8000, 0, null),
    )

    @Test
    fun `carrega eventos e sai do estado de loading`() = runTest {
        every { observeEvents() } returns flowOf(events)

        val viewModel = EventsViewModel(observeEvents)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.events).isEqualTo(events)
    }

    @Test
    fun `clicar em evento emite efeito de navegacao`() = runTest {
        every { observeEvents() } returns flowOf(events)
        val viewModel = EventsViewModel(observeEvents)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(EventsIntent.EventClicked("evt-1"))
            assertThat(awaitItem()).isEqualTo(EventsEffect.OpenEventDetail("evt-1"))
        }
    }
}
