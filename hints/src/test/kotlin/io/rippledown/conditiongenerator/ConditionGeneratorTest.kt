package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionConstructors
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionGeneratorTest {
    private lateinit var attributeFor: AttributeFor
    private lateinit var attribute: Attribute
    private lateinit var generator: ConditionGenerator
    private val constructors = ConditionConstructors()
    private val atttributeName = "x"
    private val userExpression = "whatever the user entered"


    @BeforeEach
    fun setUp() {
        attributeFor = mockk<AttributeFor>()
        attribute = mockk<Attribute>()
        every { attributeFor(atttributeName) } returns attribute
        generator = ConditionGenerator(attributeFor)
    }

    @Test
    fun `should generate Episodic condition for a no-arg predicate and no-arg signature`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(High::class.simpleName!!, listOf()),
            FunctionSpecification(Current::class.simpleName!!, listOf())
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe EpisodicCondition(null, attribute, High, Current, userExpression)
    }

    @Test
    fun `should generate Episodic condition for a 1-arg predicate and no-arg signature`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(Contains::class.simpleName!!, listOf("pending")),
            FunctionSpecification(Current::class.simpleName!!, listOf())
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe EpisodicCondition(null, attribute, Contains("pending"), Current, userExpression)
    }


    @Test
    fun `should generate Episodic condition for a no-arg predicate and 1-arg signature`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(Low::class.simpleName!!, listOf()),
            FunctionSpecification(AtLeast::class.simpleName!!, listOf("42"))
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe EpisodicCondition(null, attribute, Low, AtLeast(42), userExpression)
    }

    @Test
    fun `should generate Episodic condition for a 1-arg predicate and 1-arg signature`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(Is::class.simpleName!!, listOf("pending")),
            FunctionSpecification(AtLeast::class.simpleName!!, listOf("42"))
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe EpisodicCondition(null, attribute, Is("pending"), AtLeast(42), userExpression)
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

    /*@Test
    fun `should generate condition from ConditionStructure`() {
        val userExpression = "whatever the user entered"
        with(constructors) {
            generator.conditionFor(attributeName = "x", userExpression, "All", "High") shouldBe All(
                High(attribute, userExpression)
            )
            generator.conditionFor(attributeName = "x", userExpression, "AtMost", "3", "High") shouldBe AtMost(
                3,
                High(attribute, userExpression)
            )
            generator.conditionFor(attributeName = "x", userExpression, "No", "High") shouldBe No(
                High(attribute, userExpression)
            )
        }
    }*/

}