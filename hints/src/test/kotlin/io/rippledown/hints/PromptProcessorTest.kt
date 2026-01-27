package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.hints.ConditionService.CASE_STRUCTURE_PREDICATES
import io.rippledown.hints.ConditionService.EPISODIC_PREDICATES
import io.rippledown.hints.ConditionService.EPISODIC_SIGNATURES
import io.rippledown.hints.ConditionService.INPUT_EXPRESSIONS
import io.rippledown.hints.ConditionService.MULTIPLE_EXPRESSION_EXAMPLES
import io.rippledown.hints.ConditionService.PROMPT_TEMPLATE
import io.rippledown.hints.ConditionService.SERIES_PREDICATES
import io.rippledown.hints.ConditionService.SINGLE_EXPRESSION_EXAMPLES
import io.rippledown.hints.ConditionService.promptFor
import kotlin.test.Test

class PromptProcessorTest {
    @Test
    fun `prompt should contain the input and not contain any placeholder keys`() {
        //Given
        val input = "What is the highest mountain in the world?"

        //When
        val prompt = promptFor(input)

        //Then
        prompt shouldContain input
        prompt shouldNotContainAny listOf(
            EPISODIC_PREDICATES,
            SERIES_PREDICATES,
            CASE_STRUCTURE_PREDICATES,
            EPISODIC_SIGNATURES,
            SINGLE_EXPRESSION_EXAMPLES,
            MULTIPLE_EXPRESSION_EXAMPLES,
            PROMPT_TEMPLATE,
            INPUT_EXPRESSIONS
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