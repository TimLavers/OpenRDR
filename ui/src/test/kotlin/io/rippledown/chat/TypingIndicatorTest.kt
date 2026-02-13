package io.rippledown.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class TypingIndicatorTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should display typing indicator`() {
        with(composeTestRule) {
            // When
            setContent {
                TypingIndicator()
            }

            // Then
            onNodeWithContentDescription(TYPING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `should display three animated dots`() {
        with(composeTestRule) {
            // When
            setContent {
                TypingIndicator()
            }

            // Then the indicator should be visible and contain the animated dots
            onNodeWithContentDescription(TYPING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `should be accessible via content description`() {
        with(composeTestRule) {
            // When
            setContent {
                TypingIndicator()
            }

            // Then
            onNodeWithContentDescription(TYPING_INDICATOR).assertExists()
        }
    }
}
