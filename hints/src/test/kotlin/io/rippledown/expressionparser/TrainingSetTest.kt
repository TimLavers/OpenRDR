package io.rippledown.expressionparser

import io.kotest.matchers.shouldBe
import io.rippledown.conditiongenerator.spec
import io.rippledown.llm.trainingSet
import org.junit.jupiter.api.Test

class TrainingSetTest {
    @Test
    fun `should generate a list of examples for the LLM`() {
        // Given
        val trainingSet = trainingSet("/training_set_for_test.txt")

        // Then
        trainingSet shouldBe """
            Input: elevated x
            Output: ${spec(predicateName = "High")}
            Input: excessive x
            Output: ${spec(predicateName = "High")}
            Input: reduced x
            Output: ${spec(predicateName = "Low")}
            Input: lowered x
            Output: ${spec(predicateName = "Low")}
            Input: x is 6
            Output: ${spec(predicateName = "Is", predicateParameters = listOf("6"))}
            Input: x equals 6
            Output: ${spec(predicateName = "Is", predicateParameters = listOf("6"))}
            Input: x is the same as 6
            Output: ${spec(predicateName = "Is", predicateParameters = listOf("6"))}
        """.trimIndent()
    }
}