package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.edit.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleMakerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var allSuggestions: List<SuggestedCondition>
    private lateinit var conditionsShown: List<String>
    private lateinit var suggestionConditions: List<Condition>
    private val notes = Attribute(99, "Notes")

    @Before
    fun setUp() {
        allSuggestions = (1..5).map { index ->
            nonEditableSuggestion(index, notes, "$index")
        }
        conditionsShown = allSuggestions.map { it.asText() }
        suggestionConditions = allSuggestions.map { it.initialSuggestion() }
    }

    @Test
    fun `the available conditions should initially be all the conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, mockk(relaxed = true))
            }

            //Then
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `selecting an available condition should append it to the list of selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, mockk(relaxed = true))
            }

            //When
            clickAvailableCondition(2)

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(suggestionConditions[2].asText()))
        }
    }

    @Test
    fun `selecting an available condition with text should append it to the list of selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, mockk(relaxed = true))
            }

            //When
            clickAvailableConditionWithText(conditionsShown[2])

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(suggestionConditions[2].asText()))
        }
    }

    @Test
    fun `should call handler when the done button is clicked`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(2)

            //When
            clickFinishRuleButton()

            //Then
            verify { handler.onDone(listOf(suggestionConditions[2])) }
        }
    }

    @Test
    fun `should call handler when conditions are added`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            clickAvailableConditions(listOf(conditionsShown[1], conditionsShown[2]))

            //Then
            verify { handler.onUpdateConditions(listOf(suggestionConditions[1], suggestionConditions[2])) }
        }
    }

    @Test
    fun `should call handler when conditions are removed`() {
        val handler = mockk<RuleMakerHandler>(relaxed = true)
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableConditions(conditionsShown)

            //When
            clickSelectedConditions(listOf(allSuggestions[1].asText(), allSuggestions[2].asText()))

            //Then
            verify { handler.onUpdateConditions(listOf(suggestionConditions[0], suggestionConditions[3], suggestionConditions[4])) }
        }
    }
}
class RuleMakerWithEditableSuggestionsTest {
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
            editableSuggestion(notes, "$index")
        }
        conditionsShown = allSuggestions.map { it.asText() }
        suggestionConditions = allSuggestions.map { it.initialSuggestion() }
        handler = mockk<RuleMakerHandler>(relaxed = true)
    }

    @Test
    fun `editable conditions are correctly displayed`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `clicking an editable condition launches the condition editor`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(2)
            val newValue = "new value"
            enterNewVariableValueInConditionEditor(newValue)
            clickConditionEditorOkButton()

            val editResult = allSuggestions[2].editableCondition()!!.condition(newValue)
            verify { handler.onUpdateConditions(listOf(editResult)) }
            requireSelectedConditionsToBeDisplayed(listOf( editResult.asText()))
        }
    }

    @Test
    fun `an editable condition can be used without alteration`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(1)
            clickConditionEditorOkButton()
            verify { handler.onUpdateConditions(listOf( suggestionConditions[1])) }
            requireSelectedConditionsToBeDisplayed(listOf( conditionsShown[1]))
        }
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
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(0)
            enterNewVariableValueInConditionEditor("whatever")
            clickConditionEditorOkButton()

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
            verify { handler.onUpdateConditions(listOf( suggestionConditions[0])) }

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
            val newValue = "new value"
            enterNewVariableValueInConditionEditor(newValue)
            clickConditionEditorOkButton()
            val editResult = allSuggestions[2].editableCondition()!!.condition(newValue)
            verify { handler.onUpdateConditions(listOf( editResult)) }

            // Remove the added condition
            clickSelectedConditions(listOf( editResult.asText()))

            verify { handler.onUpdateConditions(listOf()) }
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `condition editing can be cancelled`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(0)
            clickConditionEditorCancelButton()
            requireAvailableConditionsToBeDisplayed(conditionsShown)
            requireSelectedConditionsToBeDisplayed(listOf())

        }
    }
}

fun editableSuggestion(attribute: Attribute, text: String): EditableSuggestedCondition {
    val editableCondition = EditableContainsCondition(attribute, text)
    return EditableSuggestedCondition(editableCondition)
}

fun nonEditableSuggestion(id: Int?, attribute: Attribute, text: String): NonEditableSuggestedCondition {
    val initialSuggestion = containsText(id, attribute, text)
    return NonEditableSuggestedCondition(initialSuggestion)
}
fun main() {
    val notes = Attribute(99, "Notes")
    val handler = mockk<RuleMakerHandler>(relaxed = true)
    val conditions = (1..10).map { index ->
       nonEditableSuggestion(index, notes,"condition $index")
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            RuleMaker(conditions, handler)
        }
    }
}