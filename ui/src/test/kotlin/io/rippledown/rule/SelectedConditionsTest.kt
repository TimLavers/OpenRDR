package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.Current
import org.junit.Rule
import org.junit.Test

class SelectedConditionsTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    val conditions = (1..5).map { index ->
        val attribute = Attribute(index, "Surf $index")
        EpisodicCondition(attribute, Low, Current)
    }

    @Test
    fun `should show the selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                SelectedConditions(conditions, mockk())
            }

            //Then
            requireSelectedConditionsToBeDisplayed(conditions.map { it.asText() })
        }
    }

    @Test
    fun `should call the handler when the remove icon associated with a selected condition is clicked`() {
        val handler = mockk<SelectedConditionsHandler>()

        with(composeTestRule) {
            //Given
            setContent {
                SelectedConditions(conditions, handler)
            }
            //When
            removeSelectedCondition(2)
            waitForIdle()

            //Then
            verify { handler.onRemoveCondition(conditions[2]) }
        }
    }

}