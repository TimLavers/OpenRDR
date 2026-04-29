package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.edit.SuggestedCondition
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * @author Cascade AI
 */
class SuggestedConditionsHandlerTest {
    lateinit var case: RDRCase
    lateinit var ruleService: RuleService
    lateinit var suggestionsBuffer: SuggestionsBuffer
    lateinit var handler: SuggestedConditionsHandler

    @BeforeTest
    fun setUp() {
        case = mockk()
        ruleService = mockk()
        suggestionsBuffer = SuggestionsBuffer()
        handler = SuggestedConditionsHandler(case, ruleService, suggestionsBuffer)
    }

    @Test
    fun `should return error when no rule session is active`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns false

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe SuggestedConditionsHandler.NO_ACTIVE_RULE_SESSION_ERROR
    }

    @Test
    fun `should return message when no suggestions are available`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.currentRuleSessionConditionTexts() } returns emptySet()
        every { ruleService.conditionHintsForCase(case) } returns ConditionList()

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe "No suggested conditions available for this case."
        suggestionsBuffer.suggestions shouldBe emptyList()
    }

    @Test
    fun `should buffer suggestions and return delivery instruction`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.currentRuleSessionConditionTexts() } returns emptySet()
        val suggestion1 = mockk<SuggestedCondition>()
        every { suggestion1.asText() } returns "wave height is \"2\""
        every { suggestion1.isEditable() } returns false
        val suggestion2 = mockk<SuggestedCondition>()
        every { suggestion2.asText() } returns "case is for a single date"
        every { suggestion2.isEditable() } returns false
        every { ruleService.conditionHintsForCase(case) } returns ConditionList(listOf(suggestion1, suggestion2))

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe SuggestedConditionsHandler.SUGGESTIONS_DELIVERED_INSTRUCTION
        suggestionsBuffer.suggestions shouldBe listOf(
            "wave height is \"2\"",
            "case is for a single date"
        )
    }

    @Test
    fun `should mark editable suggestions`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.currentRuleSessionConditionTexts() } returns emptySet()
        val suggestion = mockk<SuggestedCondition>()
        every { suggestion.asText() } returns "wave height >= 1.5"
        every { suggestion.isEditable() } returns true
        every { ruleService.conditionHintsForCase(case) } returns ConditionList(listOf(suggestion))

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe SuggestedConditionsHandler.SUGGESTIONS_DELIVERED_INSTRUCTION
        suggestionsBuffer.suggestions shouldBe listOf("wave height >= 1.5 [editable]")
    }

    @Test
    fun `should filter out already added conditions`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.currentRuleSessionConditionTexts() } returns setOf("wave height is \"2\"")
        val suggestion1 = mockk<SuggestedCondition>()
        every { suggestion1.asText() } returns "wave height is \"2\""
        val suggestion2 = mockk<SuggestedCondition>()
        every { suggestion2.asText() } returns "case is for a single date"
        every { suggestion2.isEditable() } returns false
        every { ruleService.conditionHintsForCase(case) } returns ConditionList(listOf(suggestion1, suggestion2))

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe SuggestedConditionsHandler.SUGGESTIONS_DELIVERED_INSTRUCTION
        suggestionsBuffer.suggestions shouldBe listOf("case is for a single date")
    }

    @Test
    fun `should return no suggestions message when all conditions already added`() = runTest {
        // Given
        every { ruleService.isRuleSessionActive() } returns true
        every { ruleService.currentRuleSessionConditionTexts() } returns setOf("wave height is \"2\"")
        val suggestion = mockk<SuggestedCondition>()
        every { suggestion.asText() } returns "wave height is \"2\""
        every { ruleService.conditionHintsForCase(case) } returns ConditionList(listOf(suggestion))

        // When
        val result = handler.handle(emptyMap())

        // Then
        result shouldBe "No suggested conditions available for this case."
        suggestionsBuffer.suggestions shouldBe emptyList()
    }
}
