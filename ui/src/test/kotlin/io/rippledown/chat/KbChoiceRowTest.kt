package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class KbChoiceRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should display one chip per name`() {
        with(composeTestRule) {
            // Given
            val names = listOf("Thyroids", "Glucose", "Zoo Animals")

            // When
            setContent {
                KbChoiceRow(names = names, index = 0)
            }

            // Then
            names.forEach { name ->
                onNodeWithContentDescription("$KB_CHOICE_ITEM$name").assertIsDisplayed()
            }
        }
    }

    @Test
    fun `should call onChosen with the name when a chip is clicked`() {
        with(composeTestRule) {
            // Given
            var clickedName: String? = null
            val names = listOf("Thyroids", "Glucose")

            // When
            setContent {
                KbChoiceRow(names = names, index = 0) { name -> clickedName = name }
            }
            onNodeWithContentDescription("$KB_CHOICE_ITEM${names[1]}").performClick()

            // Then
            assert(clickedName == "Glucose") { "Expected 'Glucose' but was '$clickedName'" }
        }
    }

    @Test
    fun `should call onChosen with the correct name when one of multiple chips is clicked`() {
        with(composeTestRule) {
            // Given
            var clickedName: String? = null
            val names = listOf("Thyroids", "Glucose", "Zoo Animals")

            // When
            setContent {
                KbChoiceRow(names = names, index = 0) { name -> clickedName = name }
            }
            onNodeWithContentDescription("$KB_CHOICE_ITEM${names[2]}").performClick()

            // Then
            assert(clickedName == "Zoo Animals") { "Expected 'Zoo Animals' but was '$clickedName'" }
        }
    }

    @Test
    fun `should display the bot content description with the index`() {
        with(composeTestRule) {
            // Given
            val names = listOf("Thyroids")
            val index = 3

            // When
            setContent {
                KbChoiceRow(names = names, index = index)
            }

            // Then
            onNodeWithContentDescription("$BOT$index").assertIsDisplayed()
        }
    }

    @Test
    fun `should not show kb choice row from a different index`() {
        with(composeTestRule) {
            // Given
            val names = listOf("Thyroids")
            val index = 1

            // When
            setContent {
                KbChoiceRow(names = names, index = index)
            }

            // Then
            onAllNodesWithContentDescription("$KB_CHOICE_LIST$0", substring = true).assertCountEquals(0)
            onNodeWithContentDescription("$KB_CHOICE_LIST$index", substring = true).assertIsDisplayed()
        }
    }
}
