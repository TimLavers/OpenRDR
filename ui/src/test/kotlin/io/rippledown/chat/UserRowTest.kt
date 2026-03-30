package io.rippledown.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class UserRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should display the user content description`() {
        with(composeTestRule) {
            // Given
            val index = 0

            // When
            setContent {
                UserRow(text = "Hello", index = index)
            }

            // Then
            onNodeWithContentDescription("$USER$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should use the correct content description based on the index`() {
        with(composeTestRule) {
            // Given
            val index = 5

            // When
            setContent {
                UserRow(text = "Hello", index = index)
            }

            // Then
            onNodeWithContentDescription("$USER$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should display the message text`() {
        with(composeTestRule) {
            // Given
            val text = "Add the comment: Beach time!"

            // When
            setContent {
                UserRow(text = text, index = 0)
            }

            // Then
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `should not show edit icon for a non-editable message`() {
        with(composeTestRule) {
            // Given
            val text = "Sun is in case"

            // When
            setContent {
                UserRow(text = text, index = 0)
            }

            // Then
            onNodeWithText(text).assertIsDisplayed()
            onNodeWithContentDescription("editable").assertDoesNotExist()
        }
    }

    @Test
    fun `should strip the editable marker and show edit icon for an editable message`() {
        with(composeTestRule) {
            // Given
            val text = "Waves ≥ 1.5$EDITABLE_MARKER"

            // When
            setContent {
                UserRow(text = text, index = 0)
            }

            // Then
            onNodeWithText("Waves ≥ 1.5").assertIsDisplayed()
            onNodeWithContentDescription("editable", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `should not display the editable marker text`() {
        with(composeTestRule) {
            // Given
            val text = "Waves ≥ 1.5$EDITABLE_MARKER"

            // When
            setContent {
                UserRow(text = text, index = 0)
            }

            // Then
            onNodeWithText(text).assertDoesNotExist()
        }
    }

    @Test
    fun `should display a message containing special characters`() {
        with(composeTestRule) {
            // Given
            val text = "Sun is \"hot\""

            // When
            setContent {
                UserRow(text = text, index = 0)
            }

            // Then
            onNodeWithText(text).assertIsDisplayed()
            onNodeWithContentDescription("editable").assertDoesNotExist()
        }
    }
}
