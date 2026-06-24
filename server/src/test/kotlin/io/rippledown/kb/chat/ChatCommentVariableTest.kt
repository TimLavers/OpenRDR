package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.CommentVariable
import kotlin.test.Test

class ChatCommentVariableTest {
    private val ruleService = mockk<RuleService>()

    @Test
    fun `toCommentVariables should resolve attribute names to ids`() {
        // Given
        val variables = listOf(
            ChatCommentVariable(attributeName = "Glucose"),
            ChatCommentVariable(attributeName = "TSH")
        )
        every { ruleService.attributeForName("Glucose") } returns Attribute(1, "Glucose")
        every { ruleService.attributeForName("TSH") } returns Attribute(2, "TSH")

        // When
        val result = variables.toCommentVariables(ruleService)

        // Then
        result shouldBe listOf(CommentVariable(1), CommentVariable(2))
    }

    @Test
    fun `toCommentVariables should handle null attributeName`() {
        // Given
        val variables = listOf(ChatCommentVariable(attributeName = null))

        // When
        val result = variables.toCommentVariables(ruleService)

        // Then
        result shouldBe listOf(CommentVariable(UNRESOLVED_ATTRIBUTE_ID))
    }

    @Test
    fun `toCommentVariables should use sentinel id for unresolved attribute`() {
        // Given
        val variables = listOf(ChatCommentVariable(attributeName = "NonExistent"))
        every { ruleService.attributeForName("NonExistent") } returns null

        // When
        val result = variables.toCommentVariables(ruleService)

        // Then
        result shouldBe listOf(CommentVariable(UNRESOLVED_ATTRIBUTE_ID))
    }

    @Test
    fun `toCommentVariables should handle empty list`() {
        // Given
        val variables = emptyList<ChatCommentVariable>()

        // When
        val result = variables.toCommentVariables(ruleService)

        // Then
        result shouldBe emptyList()
    }

    @Test
    fun `toCommentVariables should handle mix of resolved and unresolved`() {
        // Given
        val variables = listOf(
            ChatCommentVariable(attributeName = "Glucose"),
            ChatCommentVariable(attributeName = "NonExistent"),
            ChatCommentVariable(attributeName = "TSH")
        )
        every { ruleService.attributeForName("Glucose") } returns Attribute(1, "Glucose")
        every { ruleService.attributeForName("NonExistent") } returns null
        every { ruleService.attributeForName("TSH") } returns Attribute(2, "TSH")

        // When
        val result = variables.toCommentVariables(ruleService)

        // Then
        result shouldBe listOf(
            CommentVariable(1),
            CommentVariable(UNRESOLVED_ATTRIBUTE_ID),
            CommentVariable(2)
        )
    }
}
