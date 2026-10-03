@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class WarningRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should render the warning text`() {
        with(composeTestRule) {
            // Given
            val text = "The attribute order was not changed: Thyroids is being edited by Alice."

            // When
            setContent { WarningRow(text = text, index = 0) }

            // Then
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `content description should encode the index and the visible text`() {
        with(composeTestRule) {
            // Given
            val text = "The attribute order was not changed: Thyroids is being edited by Alice."
            val index = 3

            // When
            setContent { WarningRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("$WARNING$index:$text").assertIsDisplayed()
            onNodeWithContentDescription("$WARNING$index:$text").assertTextEquals(text)
        }
    }
}
