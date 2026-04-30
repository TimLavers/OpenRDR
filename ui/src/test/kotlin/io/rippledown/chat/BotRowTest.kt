@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [BotRow]. Beyond confirming that the row displays the
 * bot text, these tests lock in the exact `contentDescription` contract
 * that cucumber polling (see `ChatPO.botRowText`) depends on.
 *
 * The contract is: the row's accessibility description equals
 * `"$BOT$index:$text"`. If this format changes, update the matching
 * logic in `ChatPO` in the same commit.
 */
class BotRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should render the bot text`() {
        with(composeTestRule) {
            // Given
            val text = "Hello, how can I help you?"

            // When
            setContent { BotRow(text = text, index = 0) }

            // Then
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `content description should encode the index and the visible text`() {
        with(composeTestRule) {
            // Given
            val text = "Would you like to add a comment?"
            val index = 4

            // When
            setContent { BotRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("$BOT$index:$text").assertIsDisplayed()
        }
    }

    @Test
    fun `content description should make the text findable by substring`() {
        with(composeTestRule) {
            // Given - the cucumber polling matches on terms like "reason"
            // appearing anywhere in the bot's visible text.
            val text = "Do you want to provide any more reasons?"
            val index = 2

            // When
            setContent { BotRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("reasons", substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun `merged semantics should expose the text for assertTextEquals`() {
        with(composeTestRule) {
            // Given - mergeDescendants = true on the Surface is what lets
            // ChatProxy use .assertTextEquals against the row's merged
            // semantics node.
            val text = "Sure! What do you need help with?"
            val index = 1

            // When
            setContent { BotRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("$BOT$index:$text").assertTextEquals(text)
        }
    }

    @Test
    fun `rows at different indices should not clash on content description`() {
        with(composeTestRule) {
            // Given
            val firstText = "First"
            val secondText = "Second"

            // When
            setContent {
                androidx.compose.foundation.layout.Column {
                    BotRow(text = firstText, index = 0)
                    BotRow(text = secondText, index = 1)
                }
            }

            // Then
            onNodeWithContentDescription("$BOT${0}:$firstText").assertIsDisplayed()
            onNodeWithContentDescription("$BOT${1}:$secondText").assertIsDisplayed()
            // Prefix-match should find exactly one row per index.
            onAllNodesWithContentDescription("$BOT${0}:", substring = true).assertCountEquals(1)
            onAllNodesWithContentDescription("$BOT${1}:", substring = true).assertCountEquals(1)
        }
    }

    @Test
    fun `content description should tolerate an empty bot text`() {
        with(composeTestRule) {
            // Given - empty strings shouldn't break the "$BOT$index:"
            // prefix that cucumber splits on.
            val index = 7

            // When
            setContent { BotRow(text = "", index = index) }

            // Then
            onNodeWithContentDescription("$BOT$index:").assertIsDisplayed()
        }
    }
}
