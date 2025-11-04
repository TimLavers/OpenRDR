package io.rippledown.kb.chat

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

    @Test
    fun `system instruction should contains the case attributes in view order`() {
        val glucose = Attribute(1, "glucose")
        val glucoseValue = AttributeWithValue(glucose, TestResult("5.1"))
        val lipids = Attribute(2, "lipids")
        val lipidValue = AttributeWithValue(lipids, TestResult("5.2"))
        val age = Attribute(3, "age")
        val ageValue = AttributeWithValue(age, TestResult("53"))
        val case = createViewableCase(CaseId(99, "Case1"), listOf(glucoseValue, lipidValue, ageValue))

        val systemPrompt = KBChatService.systemPrompt(case)

        systemPrompt shouldContain glucose.name + "\n" + lipids.name + "\n" + age.name
    }
}