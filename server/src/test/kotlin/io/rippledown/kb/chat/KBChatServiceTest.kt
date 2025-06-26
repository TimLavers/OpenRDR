package io.rippledown.kb.chat

import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.CaseTestUtils.createCase
import kotlin.test.Test

class KBChatServiceTest {
    @Test
    fun `should create system instruction`() {
        // Given
        val case = createCase("caseName")

        // When
        val systemInstruction = KBChatService.systemInstruction(case)

        // Then
        withClue("System instruction should not contain placeholders") {
            systemInstruction shouldNotContain "{{"
            systemInstruction shouldNotContain "}}"
        }

        println("System Instruction: $systemInstruction")

    }
}