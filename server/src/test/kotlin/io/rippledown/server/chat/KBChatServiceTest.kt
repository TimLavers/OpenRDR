package io.rippledown.server.chat

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.TestResult
import io.rippledown.utils.AttributeWithValue
import io.rippledown.utils.createCaseWithInterpretation
import io.rippledown.utils.createViewableCase
import kotlin.test.Test

class KBChatServiceTest {
    @Test
    fun `system instruction should not contain placeholders`() {
        // Given
        val case = createCaseWithInterpretation("Test Case")

        // When
        val systemPrompt = KBChatService.systemPrompt()

        // Then
        systemPrompt shouldNotContain "{{"
        systemPrompt shouldNotContain "}}"
    }
}