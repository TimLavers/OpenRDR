package io.rippledown.hints

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ConditionChatServiceTest {

    private lateinit var service: ConditionChatService

    @BeforeEach
    fun setUp() {
        service = ConditionChatService()
    }

    private fun cs(
        expression: String,
        attribute: String?,
        predicate: String,
        predicateParameters: List<String> = listOf(),
        signature: String,
        signatureParameters: List<String> = listOf()
    ) = ConditionSpecification(
        userExpression = expression,
        attributeName = attribute,
        predicateName = predicate,
        predicateParameters = predicateParameters,
        signatureName = signature,
        signatureParameters = signatureParameters
    )

    /**
     * Given a ConditionChatService instance initialized once
     * When multiple expressions are transformed in sequence
     * Then each expression is correctly transformed without re-initializing the service
     */
    @Test
    fun `should transform multiple expressions in a single session`() {
        runBlocking {
            // When/Then - first expression
            val result1 = service.transform("glucose is high")
            result1 shouldBe cs("glucose is high", "glucose", "High", signature = "Current")

            // When/Then - second expression
            val result2 = service.transform("x is low")
            result2 shouldBe cs("x is low", "x", "Low", signature = "Current")

            // When/Then - third expression
            val result3 = service.transform("TSH is greater than 5.0")
            result3 shouldBe cs("TSH is greater than 5.0", "TSH", "GreaterThan", listOf("5.0"), "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression indicating high/elevated value is transformed
     * Then it returns a High predicate with Current signature
     */
    @Test
    fun `should transform High condition`() {
        runBlocking {
            // When
            val result = service.transform("glucose is elevated")
            // Then
            result shouldBe cs("glucose is elevated", "glucose", "High", signature = "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression indicating low/below normal value is transformed
     * Then it returns a Low predicate with Current signature
     */
    @Test
    fun `should transform Low condition`() {
        runBlocking {
            // When
            val result = service.transform("x is below normal")
            // Then
            result shouldBe cs("x is below normal", "x", "Low", signature = "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression indicating normal/within range value is transformed
     * Then it returns a Normal predicate with Current signature
     */
    @Test
    fun `should transform Normal condition`() {
        runBlocking {
            // When
            val result = service.transform("glucose is within range")
            // Then
            result shouldBe cs("glucose is within range", "glucose", "Normal", signature = "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression with numeric equality is transformed
     * Then it returns an Is predicate with the numeric value as parameter
     */
    @Test
    fun `should transform Is condition with numeric value`() {
        runBlocking {
            // When
            val result = service.transform("x = 3.14")
            // Then
            result shouldBe cs("x = 3.14", "x", "Is", listOf("3.14"), "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression with text equality is transformed
     * Then it returns an Is predicate with the quoted text value as parameter
     */
    @Test
    fun `should transform Is condition with text value`() {
        runBlocking {
            // When
            val result = service.transform("x is pending")
            // Then
            result shouldBe cs("x is pending", "x", "Is", listOf("\"pending\""), "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression with contains is transformed
     * Then it returns a Contains predicate with the quoted text value as parameter
     */
    @Test
    fun `should transform Contains condition`() {
        runBlocking {
            // When
            val result = service.transform("glucose contains undefined")
            // Then
            result shouldBe cs("glucose contains undefined", "glucose", "Contains", listOf("\"undefined\""), "Current")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression about single episode case is transformed
     * Then it returns an IsSingleEpisodeCase predicate with null attribute and no signature
     */
    @Test
    fun `should transform IsSingleEpisodeCase condition`() {
        runBlocking {
            // When
            val result = service.transform("case has one episode")
            // Then
            result shouldBe cs("case has one episode", null, "IsSingleEpisodeCase", listOf(), "")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression about attribute presence is transformed
     * Then it returns an IsPresentInCase predicate with no signature
     */
    @Test
    fun `should transform IsPresentInCase condition`() {
        runBlocking {
            // When
            val result = service.transform("glucose is available")
            // Then
            result shouldBe cs("glucose is available", "glucose", "IsPresentInCase", listOf(), "")
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression about increasing trend is transformed
     * Then it returns an Increasing predicate with no signature
     */
    @Test
    fun `should transform Increasing condition`() {
        runBlocking {
            // When
            val result = service.transform("glucose is increasing")
            // Then
            result shouldBe cs("glucose is increasing", "glucose", "Increasing", listOf(), "")
        }
    }

    /**
     * Given the ConditionChatService companion object
     * When the system prompt is built
     * Then it contains all predicate and signature names
     */
    @Test
    fun `should build system prompt with all required sections`() {
        // When
        val prompt = ConditionChatService.buildSystemPrompt()

        // Then
        prompt shouldContainAll listOf(
            "High",
            "Low",
            "Normal",
            "Contains",
            "IsSingleEpisodeCase",
            "IsPresentInCase",
            "IsAbsentFromCase",
            "Is",
            "IsNot",
            "Increasing",
            "Decreasing",
            "Current",
            "All",
            "No",
            "AtLeast",
            "AtMost"
        )
    }

    private infix fun String.shouldContainAll(substrings: List<String>) {
        substrings.forEach { substring ->
            if (!this.contains(substring)) {
                throw AssertionError("Expected string to contain '$substring' but it did not")
            }
        }
    }

    /**
     * Given a ConditionChatService instance
     * When an expression uses a different case for an attribute name
     * Then the model uses the correct case-sensitive attribute name from the provided list
     */
    @Test
    fun `should use correct attribute name casing when attributeNames provided`() {
        runBlocking {
            // Given
            service.updateChatWithAttributeNames(listOf("glucose", "TSH", "x"))

            // When - user types "X" but attribute is "x"
            val result = service.transform("X is high")

            // Then - should use "x" not "X"
            result?.attributeName shouldBe "x"
        }
    }

    /**
     * Given a ConditionChatService instance
     * When transform is called with an empty attribute names list
     * Then it can still work normally
     */
    @Test
    fun `should handle empty attribute names list`() {
        runBlocking {
            // When
            val result = service.transform("glucose is high")

            // Then
            result shouldBe cs("glucose is high", "glucose", "High", signature = "Current")
        }
    }

    @Test
    fun `should provide attribute names when transform is called`() {
        runBlocking {
            // Given
            val response = mockk<GenerateContentResponse>()
            coEvery { response.text } returns """{"userExpression":"x is high","attributeName":"x","predicate":{"name":"High","parameters":[]},"signature":{"name":"Current","parameters":[]}}"""
            val chat = mockk<Chat>()
            coEvery { chat.sendMessage(any<String>()) } returns response

            val serviceWithMock = ConditionChatService { chat }
            val attributes = listOf("x", "glucose")

            // When
            serviceWithMock.updateChatWithAttributeNames(attributes)
            serviceWithMock.transform("x is high")

            // Then
            coVerify(exactly = 1) {
                chat.sendMessage(match<String> { it.startsWith("The attribute names defined in the knowledge base are:") })
            }
        }
    }

    @Test
    fun `should only provide attribute names once per session`() {
        runBlocking {
            // Given
            val response = mockk<GenerateContentResponse>()
            coEvery { response.text } returns """{"userExpression":"x is high","attributeName":"x","predicate":{"name":"High","parameters":[]},"signature":{"name":"Current","parameters":[]}}"""
            val chat = mockk<Chat>()
            coEvery { chat.sendMessage(any<String>()) } returns response

            val serviceWithMock = ConditionChatService { chat }
            val attributes = listOf("x", "glucose")

            // When
            serviceWithMock.updateChatWithAttributeNames(attributes)
            serviceWithMock.transform("x is high")
            serviceWithMock.transform("x is high")

            // Then
            withClue("the second transform should not send the attributes to the model") {
                coVerify(exactly = 1) {
                    chat.sendMessage(match<String> { it.startsWith("The attribute names defined in the knowledge base are:") })
                }
            }
        }
    }

    @Test
    fun `should transform condition with misspelled attribute name`() {
        runBlocking {
            // Given
            service.updateChatWithAttributeNames(listOf("glucose", "LDL", "TSH"))
            // When
            val result = service.transform("flucose is elevated")
            // Then
            result shouldBe cs("flucose is elevated", "glucose", "High", signature = "Current")
        }
    }

    @Test
    fun `buildAttributePrompt should contain attribute names each on a new line and delimited by quotes`() {
        // Given
        val attributeNames = listOf("glucose", "TSH", "LDL")

        // When
        val prompt = ConditionChatService.buildAttributePrompt(attributeNames)

        // Then
        attributeNames.forEach { name ->
            withClue("Attribute '$name' should be present in the prompt, quoted and on its own line") {
                prompt.contains("\"$name\"") shouldBe true
                // Check that the quoted attribute is on its own line (preceded by newline or start, followed by newline or end)
                val quotedName = "\"$name\""
                val lines = prompt.lines()
                lines.any { it.trim() == quotedName } shouldBe true
            }
        }
    }
}
