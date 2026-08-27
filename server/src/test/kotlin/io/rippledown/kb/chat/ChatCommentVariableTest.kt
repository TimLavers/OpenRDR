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

    /**
     * The model does not always supply the variables of a comment it asks to be
     * added or to be the replacement of another. A placeholder names its
     * attribute, so the variables are taken from the comment itself; taking the
     * model at its word left a token with no variable to substitute, which
     * rendered the comment with a raw `${}` in it.
     */
    @Test
    fun `a variable is resolved from its placeholder when the model supplies none`() {
        // Given a comment with a placeholder, and no variables from the model
        every { ruleService.attributeForName("Sun") } returns Attribute(2, "Sun")

        // When it is resolved
        val (internalComment, variables) =
            resolveCommentVariables("The air temperature is {Sun}", emptyList(), ruleService)

        // Then the placeholder becomes a token with a variable to substitute
        internalComment shouldBe "The air temperature is \${}"
        variables shouldBe listOf(CommentVariable(2))
    }

    @Test
    fun `every placeholder gets a variable when the model supplies too few`() {
        // Given a comment with two placeholders, and one variable from the model
        every { ruleService.attributeForName("Wave") } returns Attribute(1, "Wave")
        every { ruleService.attributeForName("Sun") } returns Attribute(2, "Sun")

        // When it is resolved
        val (internalComment, variables) = resolveCommentVariables(
            "The wave is {Wave} and the sun is {Sun}",
            listOf(ChatCommentVariable(attributeName = "Wave")),
            ruleService
        )

        // Then both tokens have one
        internalComment shouldBe "The wave is \${} and the sun is \${}"
        variables shouldBe listOf(CommentVariable(1), CommentVariable(2))
    }

    @Test
    fun `a comment without placeholders carries no variables, whatever the model supplies`() {
        // Given a comment that merely mentions an attribute, with a variable
        // supplied for it
        val variables = listOf(ChatCommentVariable(attributeName = "Glucose"))

        // When it is resolved
        val (internalComment, resolved) =
            resolveCommentVariables("The glucose is high.", variables, ruleService)

        // Then it has none, since it has no token to substitute
        internalComment shouldBe "The glucose is high."
        resolved shouldBe emptyList()
    }

    @Test
    fun `the model's variable is used for a placeholder that names no attribute`() {
        // Given a placeholder the KB does not know, and a usable variable for it
        every { ruleService.attributeForName("air temp") } returns null
        every { ruleService.attributeForName("Sun") } returns Attribute(2, "Sun")

        // When it is resolved
        val (_, variables) = resolveCommentVariables(
            "The air temperature is {air temp}",
            listOf(ChatCommentVariable(attributeName = "Sun")),
            ruleService
        )

        // Then the model's variable stands in for it
        variables shouldBe listOf(CommentVariable(2))
    }

    @Test
    fun `a placeholder naming nothing resolvable keeps the sentinel id`() {
        // Given a placeholder that neither the KB nor the model can resolve
        every { ruleService.attributeForName("whatever") } returns null

        // When it is resolved
        val (_, variables) = resolveCommentVariables("Refer to {whatever}", emptyList(), ruleService)

        // Then its token still has a variable, so that it renders as an
        // unresolved marker rather than as the raw token
        variables shouldBe listOf(CommentVariable(UNRESOLVED_ATTRIBUTE_ID))
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
