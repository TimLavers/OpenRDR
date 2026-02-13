package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.hints.ConditionChatService.Companion.CASE_STRUCTURE_PREDICATES
import io.rippledown.hints.ConditionChatService.Companion.EPISODIC_PREDICATES
import io.rippledown.hints.ConditionChatService.Companion.EPISODIC_SIGNATURES
import io.rippledown.hints.ConditionChatService.Companion.SERIES_PREDICATES
import io.rippledown.hints.ConditionChatService.Companion.SINGLE_EXPRESSION_EXAMPLES
import io.rippledown.hints.ConditionChatService.Companion.buildSystemPrompt
import kotlin.test.Test

class PromptProcessorTest {
    @Test
    fun `prompt should not contain any placeholder keys`() {
        //Given / When
        val prompt = buildSystemPrompt()

        //Then
        prompt shouldNotContainAny listOf(
            EPISODIC_PREDICATES,
            SERIES_PREDICATES,
            CASE_STRUCTURE_PREDICATES,
            EPISODIC_SIGNATURES,
            SINGLE_EXPRESSION_EXAMPLES,
        )
    }

    @Test
    fun `should replace placeholders`() {
        //Given
        val text = "{{input}} and {{input}} and {{input}} to {{output}}"

        //When
        val replacedText = text.replacePromptPlaceholders(mapOf("input" to "A", "output" to "B"))

        //Then
        replacedText shouldBe "A and A and A to B"
    }
}

private infix fun String.shouldNotContainAny(keys: List<String>) = keys.forEach { key ->
    this shouldNotContain key
}