package com.byconcerts.tickets.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byconcerts.data.seed.EventSeeder
import com.byconcerts.domain.usecase.ObserveEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SplashViewModel(
    private val eventSeeder: EventSeeder,
    private val observeEvents: ObserveEventsUseCase,
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            eventSeeder.seedIfEmpty()
            observeEvents().first()
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_VISIBLE_MILLIS) delay((MIN_VISIBLE_MILLIS - elapsed).milliseconds)
            _ready.value = true
        }
    }

    private companion object {
        const val MIN_VISIBLE_MILLIS = 900L
    }
}
