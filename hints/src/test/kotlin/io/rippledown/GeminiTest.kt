package io.rippledown

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GeminiTest {

    @Test
    fun `test expressions using high`() {
        // Given
        val expressions = listOf(
            "x is elevated",
            "x is above the normal range",
            "raised x",
            "elevated x",
            "high x"
        )
        val expected = "x is high"
        for (entered in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using low`() {
        // Given
        val expressions = listOf(
            "x is lowered",
            "low x",
            "x is below the normal range"
        )
        val expected = "x is low"
        for (entered in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using normal`() {
        // Given
        val expressions = listOf(
            "x is OK",
            "x is not high or low",
            "x is within the normal range"
        )
        val expected = "x is normal"
        for (entered in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using equals`() {
        // Given
        val expressions = listOf(
            "x equals 10" to "x is 10",
            "x = 3.1" to "x is 3.1",
            "x == 3.1" to "x is 3.1",
            "x is the same as y" to "x is y",
            "x is equal to y" to "x is y",
            "x identical to y" to "x is y",
            "x equals \"abc\"" to "x is \"abc\"",
            "x equals abc" to "x is \"abc\""
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using LTE`() {
        // Given
        val expressions = listOf(
            "x is less than or equal to 10" to "x <= 10",
            "x no more than y" to "x <= y",
            "x is smaller than or equal to y" to "x <= y"
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using GTE`() {
        // Given
        val expressions = listOf(
            "x is greater than or equal to 10" to "x >= 10",
            "x no less than y" to "x >= y",
            "x is greater than or equal to y" to "x >= y"
        )
        for ((entered, expected) in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using is in case`() {
        // Given
        val expressions = listOf(
            "x is available",
            "there is a value for x",
            "the case contains a value for x",
            "x has been detected"
        )
        val expected = "x is in case"
        for (entered in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }

    @Test
    fun `test expressions using is increasing`() {
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
        val expected = "x increasing"
        for (entered in expressions) {
            // When
            val actual = suggestionFor(entered)

            // Then
            actual shouldBe expected
        }
    }
}
