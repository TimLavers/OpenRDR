package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ConditionSpecificationTest {
    @Test
    fun `should deserialize from json`() {
        // Given
        val json = """
            {
              "predicate":{"name":"GreaterThanOrEqualTo","parameters":["3.14"]},
              "signature":{"name":"AtMost","parameters":["5"]}
            }
            """.trimIndent()

        // When
        val conditionStructure = fromJson(json)

        // Then
        conditionStructure shouldBe ConditionSpecification(
            FunctionSpecification("GreaterThanOrEqualTo", listOf("3.14")),
            FunctionSpecification("AtMost", listOf("5"))
        )
    }
}