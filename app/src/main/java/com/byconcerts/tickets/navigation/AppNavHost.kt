package com.byconcerts.tickets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.byconcerts.feature.checkout.checkout.CheckoutRoute
import com.byconcerts.feature.checkout.receipt.ReceiptRoute
import com.byconcerts.feature.events.detail.EventDetailRoute
import com.byconcerts.feature.events.list.EventsListRoute

/** Rotas da navegação. Centralizadas para manter o grafo legível. */
private object Routes {
    const val EVENTS = "events"
    const val EVENT_DETAIL = "events/{eventId}"
    const val CHECKOUT = "checkout/{eventId}/{quantity}"
    const val RECEIPT = "receipt/{purchaseId}"

    fun eventDetail(eventId: String) = "events/$eventId"
    fun checkout(eventId: String, quantity: Int) = "checkout/$eventId/$quantity"
    fun receipt(purchaseId: String) = "receipt/$purchaseId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.EVENTS) {

        composable(Routes.EVENTS) {
            EventsListRoute(
                onOpenEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
            )
        }

        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) { entry ->
            val eventId = entry.arguments?.getString("eventId").orEmpty()
            EventDetailRoute(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onOpenCheckout = { id, quantity -> navController.navigate(Routes.checkout(id, quantity)) },
            )
        }

        composable(
            route = Routes.CHECKOUT,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("quantity") { type = NavType.IntType },
            ),
        ) { entry ->
            val eventId = entry.arguments?.getString("eventId").orEmpty()
            val quantity = entry.arguments?.getInt("quantity") ?: 1
            CheckoutRoute(
                eventId = eventId,
                quantity = quantity,
                onBack = { navController.popBackStack() },
                onOpenReceipt = { purchaseId ->
                    navController.navigate(Routes.receipt(purchaseId)) {
                        // Comprovante não deve voltar ao checkout.
                        popUpTo(Routes.EVENT_DETAIL) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.RECEIPT,
            arguments = listOf(navArgument("purchaseId") { type = NavType.StringType }),
        ) { entry ->
            val purchaseId = entry.arguments?.getString("purchaseId").orEmpty()
            ReceiptRoute(
                purchaseId = purchaseId,
                onDone = {
                    navController.navigate(Routes.EVENTS) {
                        popUpTo(Routes.EVENTS) { inclusive = true }
                    }
                },
            )
        }
    }
}
