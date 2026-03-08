package io.rippledown.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SuggestionListRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should display the bot content description`() {
        with(composeTestRule) {
            // Given
            val suggestions = listOf("Condition A", "Condition B")
            val index = 0

            // When
            setContent {
                SuggestionListRow(suggestions = suggestions, index = index)
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
                SuggestionListRow(suggestions = suggestions, index = index)
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
                SuggestionListRow(suggestions = listOf(suggestion), index = index)
            }

            // Then
            onNodeWithText("1. $suggestion").assertIsDisplayed()
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
                SuggestionListRow(suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithText("1. TSH is normal").assertIsDisplayed()
            onNodeWithText("2. FT4 is high").assertIsDisplayed()
            onNodeWithText("3. Glucose is low").assertIsDisplayed()
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
                SuggestionListRow(suggestions = suggestions, index = index)
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
                SuggestionListRow(suggestions = suggestions, index = index)
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
                SuggestionListRow(suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithText("1. TSH is normal").assertIsDisplayed()
            onNodeWithText("2. FT4 is high").assertIsDisplayed()
            // [editable] text should not appear - it's replaced by an icon
            onNodeWithContentDescription("editable", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `should call onSuggestionClicked with the suggestion text when a suggestion is clicked`() {
        with(composeTestRule) {
            // Given
            var clickedSuggestion: String? = null
            val suggestion = "TSH is normal"
            val index = 0

            setContent {
                SuggestionListRow(
                    suggestions = listOf(suggestion),
                    index = index,
                    onSuggestionClicked = { clickedSuggestion = it }
                )
            }

            // When
            onNodeWithText("1. $suggestion").performClick()

            // Then
            assert(clickedSuggestion == suggestion) {
                "Expected '$suggestion' but was '$clickedSuggestion'"
            }
        }
    }

    @Test
    fun `should call onSuggestionClicked with the correct suggestion when one of multiple suggestions is clicked`() {
        with(composeTestRule) {
            // Given
            var clickedSuggestion: String? = null
            val suggestions = listOf("TSH is normal", "FT4 is high", "Glucose is low")
            val index = 0

            setContent {
                SuggestionListRow(
                    suggestions = suggestions,
                    index = index,
                    onSuggestionClicked = { clickedSuggestion = it }
                )
            }

            // When - click the second suggestion
            onNodeWithText("2. FT4 is high").performClick()

            // Then
            assert(clickedSuggestion == "FT4 is high") {
                "Expected 'FT4 is high' but was '$clickedSuggestion'"
            }
        }
    }

    @Test
    fun `should call onSuggestionClicked with text without editable marker when an editable suggestion is clicked`() {
        with(composeTestRule) {
            // Given
            var clickedSuggestion: String? = null
            val suggestions = listOf("TSH is normal [editable]", "FT4 is high")
            val index = 0

            setContent {
                SuggestionListRow(
                    suggestions = suggestions,
                    index = index,
                    onSuggestionClicked = { clickedSuggestion = it }
                )
            }

            // When - click the editable suggestion
            onNodeWithText("1. TSH is normal").performClick()

            // Then - should not include [editable] marker
            assert(clickedSuggestion == "TSH is normal") {
                "Expected 'TSH is normal' but was '$clickedSuggestion'"
            }
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
                SuggestionListRow(suggestions = suggestions, index = index)
            }

            // Then
            onNodeWithContentDescription("${SUGGESTION_LIST}0").assertDoesNotExist()
            onNodeWithContentDescription("$SUGGESTION_LIST$index").assertIsDisplayed()
        }
    }
}
