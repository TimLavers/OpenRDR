package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.SuggestedCondition
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleMakerWithNonReusableEditableSuggestionsTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var allSuggestions: List<SuggestedCondition>
    private lateinit var conditionsShown: List<String>
    private lateinit var suggestionConditions: List<Condition>
    private val notes = Attribute(99, "Notes")
    private lateinit var handler: RuleMakerHandler

    @Before
    fun setUp() {
        allSuggestions = (1..5).map { index ->
            nonReusableEditableSuggestion(notes, index)
        }
        conditionsShown = allSuggestions.map { it.asText() }
        suggestionConditions = allSuggestions.map { it.initialSuggestion() }
        handler = mockk<RuleMakerHandler>()
    }

    @Test
    fun `using an editable condition without alteration removes the original suggestion`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(0)
            clickConditionEditorOkButton()
            requireAvailableConditionsToBeDisplayed(conditionsShown.takeLast(conditionsShown.size - 1))
        }
    }

    @Test
    fun `using an editable condition with alteration removes the original suggestion`() {
        // Given
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            requireAvailableConditionsToBeDisplayed(conditionsShown)
            clickAvailableCondition(0)
            enterNewVariableValueInConditionEditor("0.0")

            // When
            clickConditionEditorOkButton()

            // Then
            requireAvailableConditionsToBeDisplayed(conditionsShown.takeLast(conditionsShown.size - 1))
        }
    }

    @Test
    fun `removing an editable condition that was added unaltered re-instates the original suggestion`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            // Select the first and use it without editing
            clickAvailableCondition(0)
            clickConditionEditorOkButton()
            verify { handler.onUpdateConditions(listOf(suggestionConditions[0])) }

            // Remove the added condition
            clickSelectedConditions(listOf(conditionsShown[0]))

            verify { handler.onUpdateConditions(listOf()) }
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `removing an editable condition that was added after alteration re-instates the original suggestion`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            // Select the first and use it after editing
            clickAvailableCondition(0)
            val newValue = "0.0"
            enterNewVariableValueInConditionEditor(newValue)
            clickConditionEditorOkButton()
            val editResult = allSuggestions[2].editableCondition()!!.condition(newValue)
            verify { handler.onUpdateConditions(listOf(editResult)) }

            // Remove the added condition
            clickSelectedConditions(listOf(editResult.asText()))

            verify { handler.onUpdateConditions(listOf()) }
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }
}