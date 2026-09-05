package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.chat.Conversation.Companion.CONDITION_TEXT_PARAMETER
import io.rippledown.chat.Conversation.Companion.NEW_VALUE_PARAMETER
import io.rippledown.chat.Conversation.Companion.SUGGESTION_NUMBER_PARAMETER
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SelectSuggestionHandlerTest {
    private lateinit var ruleService: RuleService
    private lateinit var case: RDRCase

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        case = mockk()
    }

    private fun nonEditableSuggestion(text: String) = mockk<SuggestedCondition>().also {
        every { it.asText() } returns text
        every { it.editableCondition() } returns null
    }

    private fun editableSuggestion(text: String, editableCondition: EditableCondition) =
        mockk<SuggestedCondition>().also {
            every { it.asText() } returns text
            every { it.editableCondition() } returns editableCondition
        }

    @Test
    fun `should add matching non-editable condition to rule session`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify { ruleService.addConditionToCurrentRuleSession(condition) }
    }

    @Test
    fun `should send cornerstone status after adding condition`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return result containing reasonId`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        val result = handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        result shouldContain "reasonId"
        result shouldContain "42"
        result shouldContain "Ok"
        result shouldContain "Cornerstone status:"
    }

    @Test
    fun `should return fallback message when no matching suggestion found`() = runTest {
        // Given
        val conditionText = "unknown condition"
        val handler = SelectSuggestionHandler(case, ruleService)
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns null

        // When
        val result = handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        result shouldContain "Could not find a matching non-editable suggestion"
        result shouldContain conditionText
        result shouldNotContain "reasonId\":42"
    }

    @Test
    fun `should resolve a suggestion by the number it was shown with`() = runTest {
        // Given a numbered list of suggestions the user has been shown
        val buffer = SuggestionsBuffer()
        buffer.shown = listOf(nonEditableSuggestion("Sun is \"hot\""), nonEditableSuggestion("Waves is \"1.5\""))
        val handler = SelectSuggestionHandler(case, ruleService, buffer)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, "Waves is \"1.5\"") } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When the user chooses the second suggestion
        handler.handle(mapOf(SUGGESTION_NUMBER_PARAMETER to 2))

        // Then that suggestion's condition is added, and no text of the model's own is used
        verify { ruleService.addConditionToCurrentRuleSession(condition) }
    }

    @Test
    fun `should ask for the number again when there is no suggestion with that number`() = runTest {
        // Given a list of two suggestions
        val buffer = SuggestionsBuffer()
        buffer.shown = listOf(nonEditableSuggestion("Sun is \"hot\""), nonEditableSuggestion("Waves is \"1.5\""))
        val handler = SelectSuggestionHandler(case, ruleService, buffer)

        // When a number outside the list is given
        val result = handler.handle(mapOf(SUGGESTION_NUMBER_PARAMETER to 7))

        // Then nothing is added and the model is told to ask the user
        result shouldBe SelectSuggestionHandler.noSuchSuggestionNumberCorrection(7, 2)
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
    }

    @Test
    fun `should ask for the value when an editable suggestion is chosen without one`() = runTest {
        // Given an editable suggestion in the list shown
        val editableCondition = mockk<EditableCondition>()
        val buffer = SuggestionsBuffer()
        buffer.shown = listOf(editableSuggestion("Waves ≤ 1.5", editableCondition))
        val handler = SelectSuggestionHandler(case, ruleService, buffer)

        // When it is chosen with no value
        val result = handler.handle(mapOf(SUGGESTION_NUMBER_PARAMETER to 1))

        // Then the model is told to ask the user for one, and nothing is added
        result shouldBe SelectSuggestionHandler.askForTheValueCorrection("Waves ≤ 1.5")
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
    }

    @Test
    fun `should substitute the value into an editable suggestion`() = runTest {
        // Given an editable suggestion in the list shown
        val editableCondition = mockk<EditableCondition>()
        val buffer = SuggestionsBuffer()
        buffer.shown = listOf(editableSuggestion("Waves ≤ 1.5", editableCondition))
        val handler = SelectSuggestionHandler(case, ruleService, buffer)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { condition.asText() } returns "Waves ≤ 1.7"
        every {
            ruleService.conditionForEditedSuggestion(case, editableCondition, "1.7")
        } returns ConditionParsingResult(condition)
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When the user's value is given with the same suggestion number
        val result = handler.handle(
            mapOf(SUGGESTION_NUMBER_PARAMETER to 1, NEW_VALUE_PARAMETER to "1.7")
        )

        // Then the server-built condition is added and reported
        verify { ruleService.addConditionToCurrentRuleSession(condition) }
        result shouldContain "Waves ≤ 1.7"
        result shouldContain "reasonId"
    }

    @Test
    fun `should report why an edited suggestion cannot be used`() = runTest {
        // Given an editable suggestion whose edited form the service refuses
        val editableCondition = mockk<EditableCondition>()
        val buffer = SuggestionsBuffer()
        buffer.shown = listOf(editableSuggestion("Waves ≥ 1.5", editableCondition))
        val handler = SelectSuggestionHandler(case, ruleService, buffer)
        every {
            ruleService.conditionForEditedSuggestion(case, editableCondition, "1.7")
        } returns ConditionParsingResult(errorMessage = CONDITION_IS_NOT_TRUE)

        // When the value is given
        val result = handler.handle(
            mapOf(SUGGESTION_NUMBER_PARAMETER to 1, NEW_VALUE_PARAMETER to "1.7")
        )

        // Then nothing is added and the reason is passed back
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        result shouldContain CONDITION_IS_NOT_TRUE
    }

    @Test
    fun `should handle empty condition text`() = runTest {
        // Given
        val handler = SelectSuggestionHandler(case, ruleService)
        every { ruleService.conditionForSuggestionText(case, "") } returns null

        // When
        val result = handler.handle(emptyMap())

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        result shouldContain "Could not find a matching non-editable suggestion"
    }
}
