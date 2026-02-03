package io.rippledown.hints

import io.kotest.matchers.shouldBe
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
            val result1 = service.transform("glucose is high", emptyList())
            result1 shouldBe cs("glucose is high", "glucose", "High", signature = "Current")

            // When/Then - second expression
            val result2 = service.transform("x is low", emptyList())
            result2 shouldBe cs("x is low", "x", "Low", signature = "Current")

            // When/Then - third expression
            val result3 = service.transform("TSH is greater than 5.0", emptyList())
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
            val result = service.transform("glucose is elevated", emptyList())
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
            val result = service.transform("x is below normal", emptyList())
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
            val result = service.transform("glucose is within range", emptyList())
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
            val result = service.transform("x = 3.14", emptyList())
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
            val result = service.transform("x is pending", emptyList())
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
            val result = service.transform("glucose contains undefined", emptyList())
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
            val result = service.transform("case has one episode", emptyList())
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
            val result = service.transform("glucose is available", emptyList())
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
            val result = service.transform("glucose is increasing", emptyList())
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
            // When - user types "X" but attribute is "x"
            val result = service.transform("X is high", listOf("glucose", "TSH", "x"))

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
            val result = service.transform("glucose is high", emptyList())

            // Then
            result shouldBe cs("glucose is high", "glucose", "High", signature = "Current")
        }
    }
}
