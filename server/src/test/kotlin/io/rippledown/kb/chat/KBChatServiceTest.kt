package io.rippledown.kb.chat

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.utils.createCaseWithInterpretation
import kotlin.test.Test

class KBChatServiceTest {
    @Test
    fun `system instruction should not contain placeholders`() {
        // Given
        val case = createCaseWithInterpretation("Test Case")

        // When
        val systemPrompt = KBChatService.systemPrompt(case)

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldNotContain "}}"

    }

    @Test
    fun `system instruction should contain the comments in the interpretation`() {
        // Given
        val comments = listOf("Go to Bondi", "Go to Malabar")
        val case = createCaseWithInterpretation("Test Case", conclusionTexts = comments)

        // When
        val systemPrompt = KBChatService.systemPrompt(case)

        // Then
        comments.forEach { systemPrompt shouldContain it }
    }
}