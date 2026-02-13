package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.rippledown.hints.ConditionSpecification.Companion.decode
import kotlin.test.Test

class ConditionSpecificationTest {
    @Test
    fun `should deserialize from json array`() {
        // Given
        val userExpression1 = "at most 5 x are greater than or equal to 3.14"
        val userExpression2 = "at least 2 x are less than 10"
        val jsonArray = """
            [
              {
                "userExpression": "$userExpression1",
                "attributeName":"x",
                "predicate":{"name":"GreaterThanOrEquals","parameters":["3.14"]},
                "signature":{"name":"AtMost","parameters":["5"]}
              },
              {
                "userExpression":"$userExpression2",
                "attributeName":"x"
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
                userExpression = userExpression1,
                attributeName = "x",
                predicate = FunctionSpecification("GreaterThanOrEquals", listOf("3.14")),
                signature = FunctionSpecification("AtMost", listOf("5"))
            ),
            ConditionSpecification(
                userExpression = userExpression2,
                attributeName = "x",
                FunctionSpecification("LessThan", listOf("10")),
                FunctionSpecification("AtLeast", listOf("2"))
            )
        )
    }

    @Test
    fun `should strip leading and trailing json text when deserializing`() {
        // Given
        val userExpression = "x is elevated"
        val jsonWithExtraText = """
            ```json
            [
              {
              "userExpression": "$userExpression",
                "attributeName":"x",
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
                userExpression = userExpression,
                attributeName = "x",
                FunctionSpecification("High"),
                FunctionSpecification("Current")
            )
        )
    }

    @Test
    fun `should strip line break in user expression when deserializing`() {
        // Given
        val userExpression = "x is elevated"
        val jsonWithExtraText = """
            ```json
            [
              {
              "userExpression": "$userExpression\r",
                "attributeName":"x",
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
                userExpression = userExpression,
                attributeName = "x",
                FunctionSpecification("High"),
                FunctionSpecification("Current")
            )
        )
    }
}