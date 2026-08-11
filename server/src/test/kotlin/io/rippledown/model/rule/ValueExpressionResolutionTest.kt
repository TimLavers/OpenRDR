package io.rippledown.model.rule

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import kotlin.test.Test

internal class ValueExpressionResolutionTest {
    private val glucose = Attribute(1, "Glucose")
    private val bmi = Attribute(10, "BMI", AttributeKind.DERIVED)
    private val definition = Formula(AttributeValue(glucose))
    private val resolver: DefinitionResolver = { attribute -> if (attribute == bmi) definition else null }

    @Test
    fun `a concrete expression resolves to itself, ignoring the resolver`() {
        val literal = Literal("diabetic")
        literal.resolvedFor(bmi, resolver) shouldBeSameInstanceAs literal

        val formula = Formula(Binary(Operator.TIMES, AttributeValue(glucose), Num(2.0)))
        formula.resolvedFor(bmi, resolver) shouldBeSameInstanceAs formula
    }

    @Test
    fun `a by-definition expression resolves to the stored definition of the attribute`() {
        ByDefinition.resolvedFor(bmi, resolver) shouldBe definition
    }

    @Test
    fun `a by-definition expression resolves to null if there is no stored definition`() {
        ByDefinition.resolvedFor(glucose, resolver).shouldBeNull()
        ByDefinition.resolvedFor(bmi, NO_DEFINITIONS).shouldBeNull()
    }
}
