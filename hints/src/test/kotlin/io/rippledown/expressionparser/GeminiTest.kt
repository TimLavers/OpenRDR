package io.rippledown.expressionparser

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

//@Ignore("Run these tests individually - the free version of the Gemini API has a rate limit of 15 / minute")
class GeminiTest {

    @Test
    fun `should tokenise expressions using representing 'case is for a single date'`() {
        // Given
        val expressions = listOf(
            "case has one date",
            "case has only one episode",
            "only one episode in the case",
            "one episode only",
            "one date only"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe "SingleEpisodeCase"
            }
        }
    }
    @Test
    fun `should tokenise expressions using 'high'`() {
        // Given
        val expressions = listOf(
            "x is elevated",
            "x is above the normal range",
            "raised x",
            "elevated x",
            "high x"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe "High"
            }
        }
    }

    @Test
    fun `should tokenise expressions using 'low'`() {
        // Given
        val expressions = listOf(
            "x is lowered",
            "low x",
            "x is below the normal range"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            actual.joinToString() shouldBe "Low"
        }
    }

    @Test
    fun `should tokenise expressions using 'normal'`() {
        // Given
        val expressions = listOf(
            "x is OK",
            "x is not high or low",
            "x is within the normal range"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            actual.joinToString() shouldBe "Normal"
        }
    }

    @Test
    fun `should tokenise expressions using 'is'`() {
        // Given
        val expressions = listOf(
            "x equals 10" to "Is, 10",
            "x = 3.1" to "Is, 3.1",
            "x == 3.1" to "Is, 3.1",
            "x is the same as y" to "Is, y",
            "x is equal to y" to "Is, y",
            "x identical to y" to "Is, y",
            "x equals \"abc\"" to "Is, \"abc\"",
            "x equals abc" to "Is, \"abc\""
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe expected
            }
        }
    }

    @Test
    fun `should tokenise expressions using LTE`() {
        // Given
        val expressions = listOf(
            "x is less than or equal to 10" to "LessThanOrEqualTo, 10",
            "x no more than y" to "LessThanOrEqualTo, y",
            "x no more than 5.5" to "LessThanOrEqualTo, 5.5",
            "x is smaller than or equal to y" to "LessThanOrEqualTo, y"
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe expected
            }
        }
    }

    @Test
    fun `should tokenise expressions using GTE`() {
        // Given
        val expressions = listOf(
            "x is greater than or equal to 10" to "GreaterThanOrEqualTo, 10",
            "x no less than y" to "GreaterThanOrEqualTo, y",
            "x no less than 5.5" to "GreaterThanOrEqualTo, 5.5",
            "x is greater than or equal to y" to "GreaterThanOrEqualTo, y"
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe expected
            }
        }
    }

    @Test
    fun `should tokenise expressions using 'is in case'`() {
        // Given
        val expressions = listOf(
            "x is available",
            "there is a value for x",
            "the case contains a value for x",
            "x has been detected"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe "Present"
            }
        }
    }

    @Test
    fun `should tokenise expressions using 'is increasing'`() {
        // Given
        val expressions = listOf(
            "x is getting bigger",
            "increasing x",
            "x is on the rise",
            "x is going up",
            "x is rising",
            "x is increasing",
            "x is growing",
            "x is getting larger"
        )
        for (entered in expressions) {
            // When
            val actual = tokensFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual.joinToString() shouldBe "Increasing"
            }
        }
    }

}
