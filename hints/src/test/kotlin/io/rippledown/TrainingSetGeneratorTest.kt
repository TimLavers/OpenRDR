package io.rippledown

import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

class TrainingSetGeneratorTest {
    @org.junit.jupiter.api.Test
    fun `should generate a training set`() {
        // Given
        val fileContents =
            """
                EXPECTED: x is high
                elevated x
                excessive x
                
                expected: x is low
                reduced x
                lowered x
            
            """.trimIndent()
        val file = createTempFile().apply {
            writeText(fileContents)
        }.toFile()

        // When
        val trainingSet = trainingSet(file)

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