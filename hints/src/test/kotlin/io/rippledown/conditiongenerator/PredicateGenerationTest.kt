package io.rippledown.conditiongenerator

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.LowByAtMostSomePercentage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PredicateGenerationTest {
    private lateinit var generator: ConditionGenerator

    @BeforeEach
    fun setUp() {
        generator = ConditionGenerator(mockk())
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
}