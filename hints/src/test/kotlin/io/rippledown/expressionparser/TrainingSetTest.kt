package io.rippledown.expressionparser

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrainingSetTest {
    @Test
    fun `should generate a training set for tokenisation`() {
        // Given
        val trainingSet = trainingSet("/training_set_for_test.txt")

        // Then
        trainingSet shouldBe """
            Input: elevated x
            Output: High
            Input: excessive x
            Output: High
            Input: reduced x
            Output: Low
            Input: lowered x
            Output: Low
            Input: x is 6
            Output: Is, 6
            Input: x equals 6
            Output: Is, 6
            Input: x is the same as 6
            Output: Is, 6
        """.trimIndent()
    }
}