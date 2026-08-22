package com.byconcerts.feature.events.di

import com.byconcerts.feature.events.detail.EventDetailViewModel
import com.byconcerts.feature.events.list.EventsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val eventsModule = module {
    viewModel { EventsViewModel(get()) }
    viewModel { (eventId: String) -> EventDetailViewModel(eventId, get()) }
}
