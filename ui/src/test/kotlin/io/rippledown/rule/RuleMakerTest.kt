package io.rippledown.rule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionConstructors
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleMakerTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var allSuggestions: List<SuggestedCondition>
    private lateinit var conditionsShown: List<String>
    private lateinit var suggestionConditions: List<Condition>
    private lateinit var handler: RuleMakerHandler
    private val notes = Attribute(99, "Notes")

    @Before
    fun setUp() {
        allSuggestions = (1..5).map { index ->
            nonEditableSuggestion(index, notes, "$index")
        }
        conditionsShown = allSuggestions.map { it.asText() }
        suggestionConditions = allSuggestions.map { it.initialSuggestion() }
        handler = mockk<RuleMakerHandler>(relaxed = true)
        every { handler.conditionForExpression(any()) } returns null
    }

    @Test
    fun `the available conditions should initially be all the conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //Then
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `the available conditions should be filtered by the text entered`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            enterTextIntoConditionFilter("3")

            //Then
            requireAvailableConditionsToBeDisplayed(listOf("Notes contains \"3\""))
        }
    }

    @Test
    fun `if the tip is an available condition, then it should always be included`() {
        every { handler.conditionForExpression(any()) } returns suggestionConditions[2]
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            enterTextIntoConditionFilter("Notes has a 3")

            //Then
            requireAvailableConditionsToBeDisplayed(listOf("Notes contains \"3\""))
        }
    }

    @Test
    fun `should not generate a tip if the entered expression is blank`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //Then
            verify(exactly = 0) { handler.conditionForExpression(any()) }
        }
    }

    @Test
    fun `if the tip is not an available condition, then it should not be included`() {
        every { handler.conditionForExpression(any()) } returns null
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            enterTextIntoConditionFilter("elevated surf")

            //Then
            requireAvailableConditionsToBeDisplayed(listOf())
        }
    }

    @Test
    fun `if the tip is not an available condition, then show conditions matching the filter text`() {
        every { handler.conditionForExpression(any()) } returns null
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            enterTextIntoConditionFilter("3")

            //Then
            requireAvailableConditionsToBeDisplayed(listOf("Notes contains \"3\""))
        }
    }

    @Test
    fun `there should be no available conditions if none match the filter text entered`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }

            //When
            enterTextIntoConditionFilter("42")

            //Then
            requireAvailableConditionsToBeDisplayed(listOf())
        }
    }

    @Test
    fun `selecting an available condition should append it to the list of selected conditions`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
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
                RuleMaker(allSuggestions, handler)
            }

            //When
            clickAvailableConditionWithText(conditionsShown[2])

            //Then
            requireSelectedConditionsToBeDisplayed(listOf(suggestionConditions[2].asText()))
        }
    }

    @Test
    fun `should call handler when the done button is clicked`() {
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
    fun `should call handler when the cancel button is clicked`() {
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(2)

            //When
            clickCancelRuleButton()

            //Then
            verify { handler.onCancel() }
        }
    }

    @Test
    fun `should call handler when conditions are added`() {
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
        with(composeTestRule) {
            //Given
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableConditions(conditionsShown)

            //When
            clickSelectedConditions(listOf(allSuggestions[1].asText(), allSuggestions[2].asText()))

            //Then
            verify {
                handler.onUpdateConditions(
                    listOf(
                        suggestionConditions[0],
                        suggestionConditions[3],
                        suggestionConditions[4]
                    )
                )
            }
        }
    }
}

class RuleMakerWithReusableEditableSuggestionsTest {
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
            reusableEditableSuggestion(notes, "$index")
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
            requireSelectedConditionsToBeDisplayed(listOf(editResult.asText()))
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
            verify { handler.onUpdateConditions(listOf(suggestionConditions[1])) }
            requireSelectedConditionsToBeDisplayed(listOf(conditionsShown[1]))
        }
    }

    @Test
    fun `a contains text suggestion is still available after direct use`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(0)
            clickConditionEditorOkButton()
            requireAvailableConditionsToBeDisplayed(conditionsShown)
        }
    }

    @Test
    fun `a contains text suggestion is still available after being edited`() {
        with(composeTestRule) {
            setContent {
                RuleMaker(allSuggestions, handler)
            }
            clickAvailableCondition(0)
            enterNewVariableValueInConditionEditor("whatever")
            clickConditionEditorOkButton()
            requireAvailableConditionsToBeDisplayed(conditionsShown)
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
            val newValue = "new value"
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

    @Test
    fun `should return any available conditions that match the filter text`() = runTest {
        // Given
        val all = (1..5).map { index ->
            nonEditableSuggestion(index, notes, "$index")
        }
        val filterTest = "3"
        val conditionFor = { _: String -> null }
        val selectedConditions = listOf<Condition>()

        // When
        val available = refreshAvailableConditions(all, filterTest, selectedConditions, conditionFor)

        // Then
        available shouldBe listOf(all[2])

    }

    @Test
    fun `should return the parsed condition no other conditions match the filter`() = runTest {
        // Given
        val all = (1..5).map { index ->
            nonEditableSuggestion(index, notes, "$index")
        }
        val filterTest = "nothing will match this"
        val parsed = nonEditableSuggestion(attribute = notes, text = "Bondi")
        val conditionFor = { _: String -> parsed.initialSuggestion }
        val selectedConditions = listOf<Condition>()

        // When
        val available = refreshAvailableConditions(all, filterTest, selectedConditions, conditionFor)

        // Then
        available shouldBe listOf(parsed)
    }

    @Test
    fun `should return no available conditions if none match the filter and the filter cannot be parsed to a condition`() =
        runTest {
        // Given
        val all = (1..5).map { index ->
            nonEditableSuggestion(index, notes, "$index")
        }
        val filterTest = "nothing will match this"
            val conditionFor = { _: String -> null }
        val selectedConditions = listOf<Condition>()

        // When
        val available = refreshAvailableConditions(all, filterTest, selectedConditions, conditionFor)

        // Then
        available shouldBe emptyList()
    }
}

fun reusableEditableSuggestion(attribute: Attribute, text: String): EditableSuggestedCondition {
    val editableCondition = EditableContainsCondition(attribute, text)
    return EditableSuggestedCondition(editableCondition)
}

fun nonReusableEditableSuggestion(attribute: Attribute, text: Int): EditableSuggestedCondition {
    val editableCondition =
        EditableGreaterThanEqualsCondition(attribute, EditableValue(text.toString(), Type.Real), Current)
    return EditableSuggestedCondition(editableCondition)
}

fun nonEditableSuggestion(id: Int? = null, attribute: Attribute, text: String): NonEditableSuggestedCondition {
    val initialSuggestion = containsText(id, attribute, text)
    return NonEditableSuggestedCondition(initialSuggestion)
}

fun main() {
    val notes = Attribute(0, "Notes")
    val waves = Attribute(1, "Waves")
    val handler = mockk<RuleMakerHandler>(relaxed = true)
    every { handler.conditionForExpression(any()) } answers {
        Thread.sleep(1000)
        ConditionConstructors().High(waves, "waves look tall enough")
    }
    val conditions = (1..10).map { index ->
        nonEditableSuggestion(index, notes, "condition $index")
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            RuleMaker(conditions, handler)
        }
    }
}