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

class AnalyticsBySourcePageTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun analyticsBySourcePage_showsSingleCategoryBreakdown() {
        composeRule.setContent {
            CaffeineTheme(dynamicColor = false) {
                AnalyticsBySourcePage(
                    uiState = AnalyticsUiState(
                        selectedRange = AnalyticsRange.LAST_30_DAYS,
                        hasData = true,
                        totalCaffeineMg = 95,
                        topSourceLabel = "Coffee",
                        sourceCategoryKeys = listOf("coffee"),
                        sourceAxisLabels = listOf("Coffee"),
                        sourceValues = listOf(95.0),
                    ),
                    onRangeSelected = {},
                    onCustomRange = { _, _ -> },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(AnalyticsSourceBarTag).assertIsDisplayed()
        composeRule.onNodeWithText("100%").assertIsDisplayed()
        composeRule.onNodeWithText("Coffee").assertIsDisplayed()
        composeRule.onNodeWithText("95 mg").assertIsDisplayed()
    }
}
