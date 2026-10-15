package com.uc.caffeine.ui.screens.analytics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uc.caffeine.ui.theme.CaffeineTheme
import com.uc.caffeine.util.AnalyticsRange
import com.uc.caffeine.util.AnalyticsUiState
import org.junit.Rule
import org.junit.Test

class AnalyticsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun analyticsMainPage_showsSummaryAndNavigationCards() {
        composeRule.setContent {
            CaffeineTheme(dynamicColor = false) {
                AnalyticsMainPage(
                    uiState = AnalyticsUiState(
                        selectedRange = AnalyticsRange.LAST_30_DAYS,
                        hasData = true,
                        totalCaffeineMg = 540,
                        averageCaffeinePerDayMg = 18,
                        safeNights = 24,
                        totalNights = 30,
                        topSourceLabel = "Coffee",
                    ),
                    expressiveBlur = false,
                    onRangeSelected = {},
                    onSourcesClick = {},
                    onBedtimeClick = {},
                    onTimeOfDayClick = {},
                )
            }
        }

        composeRule.onNodeWithText("540 mg").assertIsDisplayed()
        composeRule.onNodeWithText("Caffeine by Source").assertIsDisplayed()
        composeRule.onNodeWithText("Bedtime Impact").assertIsDisplayed()
        composeRule.onNodeWithText("When You Drink Caffeine").assertIsDisplayed()
    }

    @Test
    fun analyticsMainPage_showsEmptyStateWhenNoDataExists() {
        composeRule.setContent {
            CaffeineTheme(dynamicColor = false) {
                AnalyticsMainPage(
                    uiState = AnalyticsUiState(
                        selectedRange = AnalyticsRange.LAST_30_DAYS,
                        hasData = false,
                    ),
                    expressiveBlur = false,
                    onRangeSelected = {},
                    onSourcesClick = {},
                    onBedtimeClick = {},
                    onTimeOfDayClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("analytics_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing to chart yet").assertIsDisplayed()
    }
}
