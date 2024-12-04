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
        every { attributeFor("x") } returns attribute
        generator = ConditionGenerator(attributeFor)
    }

    @Test
    fun `should return null if no tokens`() {
        generator.conditionFor(attributeName = "x", "") shouldBe null
    }

    @Test
    fun `should return a no-parameter condition from a token`() {
        val userExpression = "single episode case"
        with(constructors) {
            generator.conditionFor(attributeName = "", userExpression, "SingleEpisodeCase") shouldBe SingleEpisodeCase(
                userExpression
            )
        }
    }

    @Test
    fun `should generate one-parameter conditions from tokens`() {
        val attributeName = "x"
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
            generator.conditionFor(attributeName = "x", userExpression, "Is", "1.1") shouldBe Is(
                attribute,
                userExpression = userExpression,
                text = "1.1"
            )
            generator.conditionFor(attributeName = "x", userExpression, "Contains", "diabetic") shouldBe Contains(
                attribute,
                userExpression = userExpression,
                text = "diabetic"
            )
            generator.conditionFor(
                attributeName = "x",
                userExpression,
                "DoesNotContain",
                "diabetic"
            ) shouldBe DoesNotContain(
                attribute,
                userExpression = userExpression,
                text = "diabetic"
            )
            generator.conditionFor(
                attributeName = "x",
                userExpression,
                "GreaterThanOrEqualTo",
                "3.14"
            ) shouldBe GreaterThanOrEqualTo(
                attribute,
                userExpression = userExpression,
                d = "3.14"
            )
            generator.conditionFor(
                attributeName = "x",
                userExpression,
                "LessThanOrEqualTo",
                "3.14"
            ) shouldBe LessThanOrEqualTo(
                attribute,
                userExpression = userExpression,
                d = "3.14"
            )
        }
    }

}