package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.LowByAtMostSomePercentage
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PredicateGenerationTest {
    private lateinit var generator: ConditionGenerator
    private val attributeFor = mockk<AttributeFor>()

    @BeforeEach
    fun setUp() {
        generator = ConditionGenerator(attributeFor)
    }

    @Test
    fun `should generate a predicate with no constructor`() {
        val spec = FunctionSpecification(High::class.simpleName!!, listOf())
        generator.predicateFrom(spec) shouldBe High
    }

    @Test
    fun `should generate a predicate with String parameter`() {
        val spec = FunctionSpecification(Contains::class.simpleName!!, listOf("notes"))
        generator.predicateFrom(spec) shouldBe Contains("notes")
    }

    @Test
    fun `should generate a predicate with Double parameter`() {
        val spec = FunctionSpecification(GreaterThanOrEquals::class.simpleName!!, listOf("42"))
        generator.predicateFrom(spec) shouldBe GreaterThanOrEquals(42.0)
    }

    @Test
    fun `should generate a predicate with Int parameter`() {
        val spec = FunctionSpecification(LowByAtMostSomePercentage::class.simpleName!!, listOf("42"))
        generator.predicateFrom(spec) shouldBe LowByAtMostSomePercentage(42)
    }

    @Test
    fun `should generate a CaseStructurePredicate that is constructed with an attribute`() {
        // Given
        val glucose = mockk<Attribute>()
        val spec = FunctionSpecification(IsPresentInCase::class.simpleName!!, listOf(""))

        // When
        val predicate = generator.predicateFrom(spec, glucose)

        // Then
        predicate shouldBe IsPresentInCase(glucose)
    }

    @Test
    fun `should generate a CaseStructurePredicate that has no constructor`() {
        // Given
        val spec = FunctionSpecification(IsSingleEpisodeCase::class.simpleName!!, listOf(""))

        // When
        val predicate = generator.predicateFrom(spec)

        // Then
        predicate shouldBe IsSingleEpisodeCase
    }
}