package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.rippledown.hints.ConditionSpecification.Companion.decode
import kotlin.test.Test

class ConditionSpecificationTest {
    @Test
    fun `should deserialize from json array`() {
        // Given
        val jsonArray = """
            [
              {
                "predicate":{"name":"GreaterThanOrEquals","parameters":["3.14"]},
                "signature":{"name":"AtMost","parameters":["5"]}
              },
              {
                "predicate":{"name":"LessThan","parameters":["10"]},
                "signature":{"name":"AtLeast","parameters":["2"]}
              }
            ]
            """.trimIndent()

        // When
        val conditionSpecs = decode(jsonArray)

        // Then
        conditionSpecs shouldBe listOf(
            ConditionSpecification(
            FunctionSpecification("GreaterThanOrEquals", listOf("3.14")),
            FunctionSpecification("AtMost", listOf("5"))
            ),
            ConditionSpecification(
                FunctionSpecification("LessThan", listOf("10")),
                FunctionSpecification("AtLeast", listOf("2"))
            )
        )
    }

    @Test
    fun `should strip leading and trailing json text when deserializing`() {
        // Given
        val jsonWithExtraText = """
            ```json
            [
              {
                "predicate": {
                  "name": "High",
                  "parameters": []
                },
                "signature": {
                  "name": "Current",
                  "parameters": []
                }
              }
            ]
            ```
            """.trimIndent()

        // When
        val conditionSpecs = decode(jsonWithExtraText)

        // Then
        conditionSpecs shouldBe listOf(
            ConditionSpecification(
                FunctionSpecification("High"),
                FunctionSpecification("Current")
            )
        )
    }
}