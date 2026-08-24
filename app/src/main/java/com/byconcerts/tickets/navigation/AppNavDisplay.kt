package com.byconcerts.tickets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.byconcerts.feature.checkout.checkout.CheckoutRoute
import com.byconcerts.feature.checkout.receipt.ReceiptRoute
import com.byconcerts.feature.events.detail.EventDetailRoute
import com.byconcerts.feature.events.list.EventsListRoute
import com.byconcerts.tickets.splash.SplashRoute

@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(SplashKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashKey> {
                SplashRoute(
                    onReady = {
                        backStack.clear()
                        backStack.add(EventsKey)
                    },
                )
            }

            entry<EventsKey> {
                EventsListRoute(
                    onOpenEvent = { eventId -> backStack.add(EventDetailKey(eventId)) },
                )
            }

            entry<EventDetailKey> { key ->
                EventDetailRoute(
                    eventId = key.eventId,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenCheckout = { eventId, quantity ->
                        backStack.add(CheckoutKey(eventId, quantity))
                    },
                )
            }

            entry<CheckoutKey> { key ->
                CheckoutRoute(
                    eventId = key.eventId,
                    quantity = key.quantity,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenReceipt = { purchaseId ->
                        backStack.removeLastOrNull()
                        backStack.add(ReceiptKey(purchaseId))
                    },
                )
            }

            entry<ReceiptKey> { key ->
                ReceiptRoute(
                    purchaseId = key.purchaseId,
                    onDone = {
                        backStack.clear()
                        backStack.add(EventsKey)
                    },
                )
            }
        },
    )
}
