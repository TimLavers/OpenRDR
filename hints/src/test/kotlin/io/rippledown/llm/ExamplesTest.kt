package io.rippledown.llm

import io.kotest.matchers.shouldBe
import io.rippledown.conditiongenerator.spec
import org.junit.jupiter.api.Test

class ExamplesTest {
    @Test
    fun `should generate examples for a single predicate`() {
        // Given
        val lines = """
            EXPECTED PREDICATE: High
            elevated x
            excessive x
        """.trimIndent()
            .split("\n")

        // When
        val examples = examplesFrom(lines)

        // Then
        examples shouldBe """
            Input: elevated x
            Output: ${spec(predicateName = "High", signatureName = "")}
            Input: excessive x
            Output: ${spec(predicateName = "High", signatureName = "")}
        """.trimIndent()
    }

    @Test
    fun `should generate examples for several predicates`() {
        // Given
        val lines = """
            EXPECTED PREDICATE: High
            elevated x
            excessive x
            
            EXPECTED PREDICATE: Low
            reduced x
            lowered x
        """.trimIndent()
            .split("\n")

        // When
        val examples = examplesFrom(lines)

        // Then
        val expectedSpecHigh = spec(predicateName = "High", signatureName = "")
        val expectedSpecLow = spec(predicateName = "Low", signatureName = "")
        examples shouldBe """
            Input: elevated x
            Output: $expectedSpecHigh
            Input: excessive x
            Output: $expectedSpecHigh
            Input: reduced x
            Output: $expectedSpecLow
            Input: lowered x
            Output: $expectedSpecLow
        """.trimIndent()
    }

    @Test
    fun `should generate a list of examples for a single predicate and signature`() {
        // Given
        val lines = """
            EXPECTED PREDICATE: High
            EXPECTED SIGNATURE: AtLeast, 42
            at least 42 elevated x
            no less than 42 excessive x
        """.trimIndent()
            .split("\n")

        // When
        val examples = examplesFrom(lines)

        // Then
        val expectedSpec = spec(predicateName = "High", signatureName = "AtLeast", signatureParameters = listOf("42"))
        examples shouldBe """
            Input: at least 42 elevated x
            Output: $expectedSpec
            Input: no less than 42 excessive x
            Output: $expectedSpec
        """.trimIndent()
    }

    @Test
    fun `should generate a list of examples for several predicates and signatures`() {
        // Given
        val lines = """
            EXPECTED PREDICATE: High
            EXPECTED SIGNATURE: Current
            elevated x
            excessive x

            EXPECTED PREDICATE: Low
            EXPECTED SIGNATURE: Current
            reduced x
            lowered x

            EXPECTED PREDICATE: Is, 6
            EXPECTED SIGNATURE: AtLeast, 42
            no less than 42 x equals 6
            at least 42 x are the same as 6
        """.trimIndent()
            .split("\n")
        val examples = examplesFrom(lines)

        // Then
        val highSpec = spec(predicateName = "High", signatureName = "Current")
        val lowSpec = spec(predicateName = "Low", signatureName = "Current")
        val isSpec = spec(
            predicateName = "Is",
            predicateParameters = listOf("6"),
            signatureName = "AtLeast",
            signatureParameters = listOf("42")
        )
        examples shouldBe """
            Input: elevated x
            Output: $highSpec
            Input: excessive x
            Output: $highSpec
            Input: reduced x
            Output: $lowSpec
            Input: lowered x
            Output: $lowSpec
            Input: no less than 42 x equals 6
            Output: $isSpec
            Input: at least 42 x are the same as 6
            Output: $isSpec
        """.trimIndent()
    }
}