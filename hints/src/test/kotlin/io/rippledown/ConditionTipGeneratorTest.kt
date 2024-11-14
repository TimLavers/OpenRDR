package io.rippledown

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionTipGeneratorTest {

    private lateinit var conditionTipGenerator: ConditionTipGenerator

    @BeforeEach
    fun setUp() {
        conditionTipGenerator = ConditionTipGenerator(setOf("Glucose", "TSH", "XYZ", "Waves"))
    }

    @Test
    fun `should suggest syntactically valid conditions`() {
        // Given
        val expectations = """
            elevated glucose -> Glucose is high
            tsh is below the normal range -> TSH is low
            xyz = 3.14159 -> XYZ is 3.14159
            xyz equals 3.14159 -> XYZ is 3.14159
            xyz is no more than 3.14159 -> XYZ <= 3.14159
            xyz is at least 3.14159 -> XYZ >= 3.14159
            xyz is a number -> XYZ is numeric
            xyz is available -> XYZ is in case
            glucose is pending -> Glucose is "pending"
            elevated waves -> Waves is high
            very tall waves -> Waves is high
        """.trimIndent()

        // Then
        expectations.split("\n").forEach { pair ->
            val (input, expected) = pair.split(" -> ")
            conditionTipGenerator.conditionTip(input) shouldBe expected
        }
    }
}