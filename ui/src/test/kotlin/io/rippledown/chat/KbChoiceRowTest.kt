package io.rippledown.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class KbChoiceRowTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `demonstration chips can be scrolled and clicked in a narrow panel`() {
        // Given
        val names = listOf("Contact Lense Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals")
        var chosen: String? = null
        composeTestRule.setContent {
            Box(Modifier.width(220.dp)) {
                KbChoiceRow(names, 0) { chosen = it }
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("$KB_CHOICE_ITEM${names.last()}")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Then
        chosen shouldBe "Zoo Animals"
    }

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
            clickedName shouldBe "Glucose"
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
            clickedName shouldBe "Zoo Animals"
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
