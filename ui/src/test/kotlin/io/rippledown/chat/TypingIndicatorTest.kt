package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

            // Then the indicator should contain three dot children
            val surface = onNodeWithContentDescription(TYPING_INDICATOR).onChildAt(0)
            surface.onChildren().assertCountEquals(3)
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

    @Test
    fun `each dot should be displayed`() {
        with(composeTestRule) {
            // When
            setContent {
                TypingIndicator()
            }

            // Then each of the three dots should be displayed
            val dotsRow = onNodeWithContentDescription(TYPING_INDICATOR).onChildAt(0)
            for (i in 0..2) {
                dotsRow.onChildAt(i).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `should have exactly one top-level child`() {
        with(composeTestRule) {
            // When
            setContent {
                TypingIndicator()
            }

            // Then the outer row should contain a single Surface
            onNodeWithContentDescription(TYPING_INDICATOR)
                .onChildren()
                .assertCountEquals(1)
        }
    }
}
