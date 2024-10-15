package io.rippledown

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrainingSetGeneratorTest {
    @Test
    fun `should generate a training set`() {
        // Given
        val trainingSet = trainingSet("/training_set_for_testing.txt")

        // Then
        trainingSet shouldBe """
            Input: elevated x
            Output: x is high
            Input: excessive x
            Output: x is high
            Input: reduced x
            Output: x is low
            Input: lowered x
            Output: x is low
        """.trimIndent()
    }
}