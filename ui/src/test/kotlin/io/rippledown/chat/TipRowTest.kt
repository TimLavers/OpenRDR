@file:OptIn(ExperimentalTestApi::class)

package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [TipRow]. Beyond confirming that the row displays the tip
 * text, these tests lock in the `contentDescription` contract
 * (`"$TIP$index:$text"`) so the row stays substring-findable like the other
 * chat rows.
 */
class TipRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should render the tip text`() {
        with(composeTestRule) {
            // Given
            val text =
                "Tip: you can include a case value in a comment by wrapping an attribute name in braces, e.g. {Wave}."

            // When
            setContent { TipRow(text = text, index = 0) }

            // Then
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `content description should encode the index and the visible text`() {
        with(composeTestRule) {
            // Given
            val text = "Tip: you can include a case value in a comment using braces, e.g. {Glucose}."
            val index = 3

            // When
            setContent { TipRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("$TIP$index:$text").assertIsDisplayed()
        }
    }

    @Test
    fun `content description should make the text findable by substring`() {
        with(composeTestRule) {
            // Given - the cucumber polling matches on the term "braces".
            val text = "Tip: you can include a case value in a comment using braces, e.g. {TSH}."
            val index = 2

            // When
            setContent { TipRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("braces", substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun `merged semantics should expose the text for assertTextEquals`() {
        with(composeTestRule) {
            // Given - mergeDescendants = true on the Surface lets tests assert
            // against the row's merged semantics node.
            val text = "Tip: wrap an attribute name in braces, e.g. {Sun}."
            val index = 1

            // When
            setContent { TipRow(text = text, index = index) }

            // Then
            onNodeWithContentDescription("$TIP$index:$text").assertTextEquals(text)
        }
    }

    @Test
    fun `rows at different indices should not clash on content description`() {
        with(composeTestRule) {
            // Given
            val firstText = "First tip"
            val secondText = "Second tip"

            // When
            setContent {
                androidx.compose.foundation.layout.Column {
                    TipRow(text = firstText, index = 0)
                    TipRow(text = secondText, index = 1)
                }
            }

            // Then
            onNodeWithContentDescription("$TIP${0}:$firstText").assertIsDisplayed()
            onNodeWithContentDescription("$TIP${1}:$secondText").assertIsDisplayed()
            onAllNodesWithContentDescription("$TIP${0}:", substring = true).assertCountEquals(1)
            onAllNodesWithContentDescription("$TIP${1}:", substring = true).assertCountEquals(1)
        }
    }
}
