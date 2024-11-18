package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionConstructors
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionGeneratorTest {
    private lateinit var attributeFor: AttributeFor
    private lateinit var attribute: Attribute
    private lateinit var generator: ConditionGenerator
    private val constructors = ConditionConstructors()

    @BeforeEach
    fun setUp() {
        attributeFor = mockk<AttributeFor>()
        attribute = mockk<Attribute>()
        every { attributeFor(any()) } returns attribute
        generator = ConditionGenerator(attributeFor)
    }

    @Test
    fun `should return null if no tokens`() {
        generator.conditionFor("", "") shouldBe null
    }

    @Test
    fun `should generate one-parameter conditions from tokens`() {
        val attributeName = "waves"
        val userExpression = "whatever the user entered"
        with(constructors) {
            generator.conditionFor(attributeName, userExpression, "Low") shouldBe Low(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "Normal") shouldBe Normal(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "High") shouldBe High(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "Present") shouldBe Present(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "Absent") shouldBe Absent(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "Numeric") shouldBe Numeric(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "NotNumeric") shouldBe NotNumeric(
                attribute,
                userExpression
            )
            generator.conditionFor(attributeName, userExpression, "Blank") shouldBe Blank(attribute, userExpression)
            generator.conditionFor(attributeName, userExpression, "NotBlank") shouldBe NotBlank(
                attribute,
                userExpression
            )
            generator.conditionFor(attributeName, userExpression, "Increasing") shouldBe Increasing(
                attribute,
                userExpression
            )
            generator.conditionFor(attributeName, userExpression, "Decreasing") shouldBe Decreasing(
                attribute,
                userExpression
            )
        }
    }

    @Test
    fun `should generate two-parameter conditions from tokens`() {
        val userExpression = "whatever the user entered"
        with(constructors) {
            generator.conditionFor("", userExpression, "Is", "1.1") shouldBe Is(attribute, "1.1", userExpression)
            generator.conditionFor("", userExpression, "Contains", "diabetic") shouldBe Contains(
                attribute,
                "diabetic",
                userExpression
            )
            generator.conditionFor("", userExpression, "DoesNotContain", "diabetic") shouldBe DoesNotContain(
                attribute,
                "diabetic",
                userExpression
            )
            generator.conditionFor("", userExpression, "GreaterThanOrEqualTo", "3.14") shouldBe GreaterThanOrEqualTo(
                attribute,
                "3.14",
                userExpression
            )
            generator.conditionFor(
                "",
                userExpression,
                "LessThanOrEqualTo",
                "3.14"
            ) shouldBe LessThanOrEqualTo(attribute, "3.14", userExpression)
        }
    }

}