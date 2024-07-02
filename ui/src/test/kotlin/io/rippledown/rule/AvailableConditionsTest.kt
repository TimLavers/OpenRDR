package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.Current
import org.junit.Rule
import org.junit.Test

class AvailableConditionsTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `should show the available conditions`() {
        val conditions = (0..9).map { index ->
            val attribute = Attribute(index, "Surf $index")
            EpisodicCondition(attribute, Low, Current)
        }

        with(composeTestRule) {
            //Given
            setContent {
                AvailableConditions(conditions, mockk())
            }

            //Then
            requireAvailableConditionsToBeDisplayed(conditions.map { it.asText() })
        }
    }

    @Test
    fun `should call the handler when an available condition is clicked by index`() {
        val conditions = (0..9).map { index ->
            val attribute = Attribute(index, "Surf $index")
            EpisodicCondition(attribute, Low, Current)
        }
        val handler = mockk<AvailableConditionsHandler>(relaxed = true)

        with(composeTestRule) {
            //Given
            setContent {
                AvailableConditions(conditions, handler)
            }
            //When
            clickAvailableCondition(2)
            waitForIdle()

            //Then
            verify { handler.onAddCondition(conditions[2]) }
        }
    }

    @Test
    fun `should call the handler when an available condition is clicked by text`() {
        val conditions = (0..9).map { index ->
            val attribute = Attribute(index, "Surf $index")
            EpisodicCondition(attribute, Low, Current)
        }
        val handler = mockk<AvailableConditionsHandler>(relaxed = true)

        with(composeTestRule) {
            //Given
            setContent {
                AvailableConditions(conditions, handler)
            }
            //When
            clickAvailableConditionWithText(conditions[2].asText())
            waitForIdle()

            //Then
            verify { handler.onAddCondition(conditions[2]) }
        }
    }
}

fun main() {
    val handler = mockk<AvailableConditionsHandler>(relaxed = true)
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val conditions = (1..100).map { index ->
                val attribute = Attribute(index, "Surf $index")
                EpisodicCondition(attribute, Low, Current)
            }
            AvailableConditions(conditions, handler)
        }
    }
}