package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
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
fun editableSuggestion(id: Int?, attribute: Attribute, text: String): EditableSuggestedCondition {
    val initialSuggestion = containsText(id, attribute, text)
    val editableCondition = EditableContainsCondition(attribute, EditableValue(text, Type.Text))
    return EditableSuggestedCondition(initialSuggestion, editableCondition)
}
fun nonEditableSuggestion(id: Int?, attribute: Attribute, text: String): FixedSuggestedCondition {
    val initialSuggestion = containsText(id, attribute, text)
    return FixedSuggestedCondition(initialSuggestion)
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