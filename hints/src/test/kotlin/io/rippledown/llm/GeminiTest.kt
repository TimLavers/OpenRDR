package io.rippledown.llm

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.conditiongenerator.spec
import org.junit.jupiter.api.Test

class GeminiTest {

    @Test
    fun `should generate 'high'`() {
        // Given
        val expressions = listOf(
            "x is elevated",
            "x is above the normal range",
            "raised x",
            "elevated x",
            "high x",
            "x es alto",
            "x es mejor que el rango normal"
        )

        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "High", signatureName = "Current")
            }
        }
    }

    @Test
    fun `should generate 'low'`() {
        // Given
        val expressions = listOf(
            "x is lowered",
            "low x",
            "x is below the normal range",
            "x es menor que el rango normal",
            "x低於正常範圍"
        )

        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Low", signatureName = "Current")
            }
        }
    }

    @Test
    fun `should generate 'normal'`() {
        // Given
        val expressions = listOf(
            "x is OK",
            "x is not high or low",
            "x is within the normal range"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            actual shouldBe spec(predicateName = "Normal", signatureName = "Current")
        }
    }

    @Test
    fun `should generate 'Is' with number`() {
        // Given
        val expressions = listOf(
            "x equals 3.1",
            "x = 3.1",
            "x == 3.1",
            "x is equal to 3.1",
        )

        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Is",
                    predicateParameters = listOf("3.1"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'Is' with unquoted text parameter`() {
        // Given
        val param = "abc"
        val expressions = listOf(
            "x equals $param",
            "x = $param",
            "x == $param",
            "x is the same as $param",
            "x is equal to $param",
            "x identical to $param",
        )

        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Is",
                    predicateParameters = listOf("\"$param\""),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'Is' with quoted text parameter`() {
        // Given
        val param = "\"abc\""
        val expressions = listOf(
            "x equals $param",
            "x = $param",
            "x == $param",
            "x is the same as $param",
            "x is equal to $param",
            "x identical to $param",
        )

        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Is",
                    predicateParameters = listOf(param),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'greater than or equals'`() {
        // Given
        val expressions = listOf(
            "x is greater than or equal to 10.0",
            "x no less than 10.0",
            "x is greater than or equal to 10.0"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "GreaterThanOrEquals",
                    predicateParameters = listOf("10.0"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'less than or equals'`() {
        // Given
        val expressions = listOf(
            "x is less than or equal to 10.0",
            "x no more than 10.0",
            "x is smaller than or equal to 10.0"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "LessThanOrEquals",
                    predicateParameters = listOf("10.0"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'at most greater than or equals'`() {
        // Given
        val expressions = listOf(
            "at most 42 x are greater than or equal to 10.1",
            "no more than 42 x results greater than or equal to 10.1",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "GreaterThanOrEquals", predicateParameters = listOf("10.1"),
                    signatureName = "AtMost", signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at most less than or equals'`() {
        // Given
        val expressions = listOf(
            "at most 42 x are less than or equal to 10.1",
            "no more than 42 x results are less than or equal to 10.1",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "LessThanOrEquals", predicateParameters = listOf("10.1"),
                    signatureName = "AtMost", signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at most low'`() {
        // Given
        val expressions = listOf(
            "at most 42 x are low",
            "there are at most 42 x results that are lowered",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Low",
                    signatureName = "AtMost",
                    signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at most high'`() {
        // Given
        val expressions = listOf(
            "at most 42 x are high",
            "there are at most 42 x results that are elevated",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "High",
                    signatureName = "AtMost",
                    signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at least greater than or equals'`() {
        // Given
        val expressions = listOf(
            "at least 42 x are greater than or equal to 10.1",
            "no fewer than 42 x results greater than or equal to 10.1",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "GreaterThanOrEquals", predicateParameters = listOf("10.1"),
                    signatureName = "AtLeast", signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at least less than or equals'`() {
        // Given
        val expressions = listOf(
            "at least 42 x are less than or equal to 10.1",
            "no fewer than 42 x results are less than or equal to 10.1",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "LessThanOrEquals", predicateParameters = listOf("10.1"),
                    signatureName = "AtLeast", signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at least low'`() {
        // Given
        val expressions = listOf(
            "at least 42 x are low",
            "there are no fewer than 42 x results that are lowered",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Low",
                    signatureName = "AtLeast",
                    signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'at least high'`() {
        // Given
        val expressions = listOf(
            "at least 42 x are high",
            "there are no fewer than 42 x results that are elevated",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "High",
                    signatureName = "AtLeast",
                    signatureParameters = listOf("42")
                )
            }
        }
    }

    @Test
    fun `should generate 'is numeric'`() {
        // Given
        val expressions = listOf(
            "x is a number",
            "numeric x",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsNumeric", signatureName = "Current")
            }
        }
    }

    @Test
    fun `should generate 'all numeric'`() {
        // Given
        val expressions = listOf(
            "all x are numeric",
            "every x is a number"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsNumeric", signatureName = "All")
            }
        }
    }

    @Test
    fun `should generate 'no numeric'`() {
        // Given
        val expressions = listOf(
            "no x is numeric",
            "none of the x are numbers"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsNumeric", signatureName = "No")
            }
        }
    }

    @Test
    fun `should generate 'contains'`() {
        // Given
        val expressions = listOf(
            "x contain pending",
            "x contains \"pending\""
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Contains",
                    predicateParameters = listOf("\"pending\""),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'all contain'`() {
        // Given
        val expressions = listOf(
            "all x contain pending",
            "every x includes \"pending\""
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Contains",
                    predicateParameters = listOf("\"pending\""),
                    signatureName = "All"
                )
            }
        }
    }

    @Test
    fun `should generate 'does not contain'`() {
        // Given
        val expressions = listOf(
            "x does not contain pending",
            "x does not include \"pending\""
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "DoesNotContain",
                    predicateParameters = listOf("\"pending\""),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'no contains'`() {
        // Given
        val expressions = listOf(
            "no x includes pending",
            "none of the x contain \"pending\"",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "Contains", predicateParameters = listOf("\"pending\""),
                    signatureName = "No"
                )
            }
        }
    }

    @Test
    fun `should generate 'all low'`() {
        // Given
        val expressions = listOf(
            "all x are low",
            "every x is low",
            "all x are below the normal range",
            "all x are lowered"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Low", signatureName = "All")
            }
        }
    }

    @Test
    fun `should generate 'no low'`() {
        // Given
        val expressions = listOf(
            "no x are low",
            "no lowered x",
            "none of the x are below the normal range",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Low", signatureName = "No")
            }
        }
    }

    @Test
    fun `should generate 'all high'`() {
        // Given
        val expressions = listOf(
            "all x are high",
            "every x is high",
            "all x are above the normal range",
            "all x are elevated",
            "all x are raised"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "High", signatureName = "All")
            }
        }
    }

    @Test
    fun `should generate 'no high'`() {
        // Given
        val expressions = listOf(
            "no x are high",
            "no elevated x",
            "none of the x are above the normal range",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "High", signatureName = "No")
            }
        }
    }

    @Test
    fun `should generate 'all normal'`() {
        // Given
        val expressions = listOf(
            "all x are normal",
            "every x is normal",
            "all x are within the normal range",
            "all x are OK",
            "all x are not high or low"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Normal", signatureName = "All")
            }
        }
    }

    @Test
    fun `should generate 'no normal'`() {
        // Given
        val expressions = listOf(
            "no x are normal",
            "every x is abnormal",
            "none of the x are within the normal range",
            "no x are OK",
            "all x results are abnormal"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Normal", signatureName = "No")
            }
        }
    }


    @Test
    fun `should generate 'case is for a single date'`() {
        // Given
        val expressions = listOf(
            "case has one date",
            "case has only one episode",
            "only one episode in the case",
            "one episode only",
            "one date only"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsSingleEpisodeCase", signatureName = "")
            }
        }
    }

    @Test
    fun `should generate 'is present in case'`() {
        // Given
        val expressions = listOf(
            "x is available",
            "there is a value for x",
            "the case contains a value for x",
            "x has been detected"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsPresentInCase", signatureName = "")
            }
        }
    }

    @Test
    fun `should generate 'is absent from case'`() {
        // Given
        val expressions = listOf(
            "x is not available",
            "there is no value for x",
            "x is missing",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "IsAbsentFromCase", signatureName = "")
            }
        }
    }

    @Test
    fun `should generate 'increasing'`() {
        // Given
        val expressions = listOf(
            "x is getting bigger",
            "increasing x",
            "x is on the rise",
            "x is going up",
            "x is rising",
            "x is increasing",
            "x is growing",
            "x is getting larger"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Increasing", signatureName = "")
            }
        }
    }

    @Test
    fun `should generate 'decreasing'`() {
        // Given
        val expressions = listOf(
            "decreasing x",
            "x is going down",
            "x is falling",
            "x is decreasing",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(predicateName = "Decreasing", signatureName = "")
            }
        }
    }

    @Test
    fun `should generate 'low by at most some percentage'`() {
        // Given
        val expressions = listOf(
            "x is low by no more than 20 percent",
            "x is low by at most 20 percent",
            "x is below normal by at most 20 percent",
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "LowByAtMostSomePercentage",
                    predicateParameters = listOf("20"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'high by at most some percentage'`() {
        // Given
        val expressions = listOf(
            "x is high by no more than 20 percent",
            "x is raised by no more than 20 percent",
            "x is above normal by no more than 20%"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "HighByAtMostSomePercentage",
                    predicateParameters = listOf("20"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'normal or high by at most some percentage'`() {
        // Given
        val expressions = listOf(
            "x is either normal or no more than 20 percent above normal",
            "x is normal or raised by no more than 20 percent",
            "x is normal or high by no more than 20%"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "NormalOrHighByAtMostSomePercentage",
                    predicateParameters = listOf("20"),
                    signatureName = "Current"
                )
            }
        }
    }

    @Test
    fun `should generate 'normal or low by at most some percentage'`() {
        // Given
        val expressions = listOf(
            "x is either normal no more than 20 percent below normal",
            "x is normal or lowered by no more than 20 percent",
            "x is normal or below normal by no more than 20%"
        )
        for (entered in expressions) {
            // When
            val actual = conditionSpecificationFor(entered)

            // Then
            withClue("Entered '$entered'") {
                actual shouldBe spec(
                    predicateName = "NormalOrLowByAtMostSomePercentage",
                    predicateParameters = listOf("20"),
                    signatureName = "Current"
                )
            }
        }
    }
}
