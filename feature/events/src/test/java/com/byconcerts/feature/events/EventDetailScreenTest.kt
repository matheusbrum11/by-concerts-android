package com.byconcerts.feature.events

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.byconcerts.domain.model.Event
import com.byconcerts.feature.events.detail.EventDetailIntent
import com.byconcerts.feature.events.detail.EventDetailScreen
import com.byconcerts.feature.events.detail.EventDetailState
import com.google.common.truth.Truth.assertThat
import com.mns.designsystem.theme.MnsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EventDetailScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val event = Event(
        id = "evt-1",
        title = "Show A",
        venue = "Arena",
        city = "SP",
        dateEpochMillis = 1_800_000_000_000,
        unitPriceInCents = 5000,
        availableQuantity = 10,
        imageUrl = null,
    )

    @Test
    fun `exibe evento e dispara compra ao clicar em Comprar`() {
        val intents = mutableListOf<EventDetailIntent>()
        composeRule.setContent {
            MnsTheme {
                EventDetailScreen(
                    state = EventDetailState(isLoading = false, event = event, quantity = 2),
                    onIntent = { intents.add(it) },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Show A").assertIsDisplayed()
        composeRule.onNodeWithText("Comprar").assertIsDisplayed().performClick()

        assertThat(intents).contains(EventDetailIntent.BuyClicked)
    }
}
