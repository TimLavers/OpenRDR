package io.rippledown.hints

import io.kotest.matchers.shouldBe
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
Output: ${ConditionSpecification("elevated x", "x", predicateName = "High", signatureName = "")}
Input: excessive x
Output: ${ConditionSpecification("excessive x", "x", predicateName = "High", signatureName = "")}
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
        examples shouldBe """
Input: elevated x
Output: ${ConditionSpecification("elevated x", "x", predicateName = "High", signatureName = "")}
Input: excessive x
Output: ${ConditionSpecification("excessive x", "x", predicateName = "High", signatureName = "")}
Input: reduced x
Output: ${ConditionSpecification("reduced x", "x", predicateName = "Low", signatureName = "")}
Input: lowered x
Output: ${ConditionSpecification("lowered x", "x", predicateName = "Low", signatureName = "")}
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
        val conditionSpecificationForElevated = ConditionSpecification(
            "at least 42 elevated x",
            "x",
            predicateName = "High",
            signatureName = "AtLeast",
            signatureParameters = listOf("42")
        )
        val conditionSpecificationForExcessive = ConditionSpecification(
            "no less than 42 excessive x",
            "x",
            predicateName = "High",
            signatureName = "AtLeast",
            signatureParameters = listOf("42")
        )
        examples shouldBe """
Input: at least 42 elevated x
Output: $conditionSpecificationForElevated
Input: no less than 42 excessive x
Output: $conditionSpecificationForExcessive
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
        examples shouldBe """
Input: elevated x
Output: ${ConditionSpecification("elevated x", "x", predicateName = "High", signatureName = "Current")}
Input: excessive x
Output: ${ConditionSpecification("excessive x", "x", predicateName = "High", signatureName = "Current")}
Input: reduced x
Output: ${ConditionSpecification("reduced x", "x", predicateName = "Low", signatureName = "Current")}
Input: lowered x
Output: ${ConditionSpecification("lowered x", "x", predicateName = "Low", signatureName = "Current")}
Input: no less than 42 x equals 6
Output: ${
            ConditionSpecification(
                "no less than 42 x equals 6",
                "x",
                predicateName = "Is",
                predicateParameters = listOf("6"),
                signatureName = "AtLeast",
                signatureParameters = listOf("42")
            )
        }
Input: at least 42 x are the same as 6
Output: ${
            ConditionSpecification(
                "at least 42 x are the same as 6",
                "x",
                predicateName = "Is",
                predicateParameters = listOf("6"),
                signatureName = "AtLeast",
                signatureParameters = listOf("42")
            )
        }
        """.trimIndent()
    }
}