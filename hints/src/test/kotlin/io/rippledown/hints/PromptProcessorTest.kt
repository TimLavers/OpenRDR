package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.hints.ConditionService.promptFor
import kotlin.test.Test

class PromptProcessorTest {
    @Test
    fun `prompt should contain the input and not contain any placeholders`() {
        //Given
        val input = "What is the highest mountain in the world?"

        //When
        val prompt = promptFor(input)

        //Then
        prompt shouldContain input
        prompt shouldNotContain "{{"
        prompt shouldNotContain "}}"
    }

    @Test
    fun `should replace placeholders`() {
        //Given
        val text = "{{input}} and {{input}} and {{input}} to {{output}}"

        //When
        val replacedText = text.replacePlaceholders(mapOf("input" to "A", "output" to "B"))

        //Then
        replacedText shouldBe "A and A and A to B"
    }
}
