package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.expressionparser.AttributeFor
import io.rippledown.model.Attribute
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionGeneratorTest {
    private lateinit var attributeFor: AttributeFor
    private lateinit var attribute: Attribute
    private lateinit var generator: ConditionGenerator
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
    fun `should generate CaseStructure condition`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(IsSingleEpisodeCase::class.simpleName!!, listOf())
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe CaseStructureCondition(null, IsSingleEpisodeCase, userExpression)
    }

    @Test
    fun `should generate Series condition`() {
        //Given
        val spec = ConditionSpecification(
            FunctionSpecification(Increasing::class.simpleName!!, listOf())
        )

        //When
        val condition = generator.conditionFor(
            attributeName = atttributeName,
            userExpression = userExpression,
            conditionSpec = spec
        )

        //Then
        condition shouldBe SeriesCondition(null, attribute, Increasing, userExpression)
    }
}