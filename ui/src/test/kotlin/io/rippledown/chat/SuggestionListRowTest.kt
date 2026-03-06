package io.rippledown.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class SuggestionListRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should display the header text`() {
        with(composeTestRule) {
            // Given
            val headerText = "Here are some suggested conditions:"
            val suggestions = listOf("Condition A", "Condition B")
            val index = 0

            // When
            setContent {
                SuggestionListRow(text = headerText, suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$BOT$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should display the suggestion list container`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("Condition A", "Condition B")
            val index = 3

            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$SUGGESTION_LIST$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should display a single suggestion as numbered text`() {
        with(composeTestRule) {
            // Given
            val suggestion = "TSH is normal"
            val index = 0

            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = listOf(suggestion), index = index)
            }

            // Then
            onNodeWithContentDescription("$SUGGESTION_LIST$index")
                .assertTextEquals("1. $suggestion")
        }
    }

    @Test
    fun `should display multiple suggestions as numbered text`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("TSH is normal", "FT4 is high", "Glucose is low")
            val index = 2
            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$SUGGESTION_LIST$index")
                .assertIsDisplayed()
                .assertTextContains("1. TSH is normal")
                .assertTextContains("2. FT4 is high")
                .assertTextContains("3. Glucose is low")
        }
    }

    @Test
    fun `should display empty text when the list is empty`() {
        with(composeTestRule) {
            // Given
            val suggestions = emptyList<String>()
            val index = 0

            // When
            setContent {
                SuggestionListRow(text = "No suggestions available.", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$BOT$index").assertIsDisplayed()
            onNodeWithContentDescription("$SUGGESTION_LIST$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should use the correct content description based on the index`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("Condition A")
            val index = 5

            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$BOT$index").assertIsDisplayed()
            onNodeWithContentDescription("$SUGGESTION_LIST$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should display editable suggestions with edit icon instead of marker text`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("TSH is normal [editable]", "FT4 is high")
            val index = 0

            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("$SUGGESTION_LIST$index")
                .assertTextContains("1. TSH is normal")
                .assertTextContains("2. FT4 is high")
            // [editable] text should not appear - it's replaced by an icon
            onNodeWithContentDescription("editable").assertIsDisplayed()
        }
    }

    @Test
    fun `should not show suggestion list from a different index`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("Condition A", "Condition B")
            val index = 1

            // When
            setContent {
                SuggestionListRow(text = "Suggestions:", suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("${SUGGESTION_LIST}0").assertDoesNotExist()
            onNodeWithContentDescription("$SUGGESTION_LIST$index").assertIsDisplayed()
        }
    }
}
