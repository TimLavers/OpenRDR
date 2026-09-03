package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.utils.defaultDate
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

internal class ValueExpressionTest {
    private val weight = Attribute(1, "weight")
    private val height = Attribute(2, "height")
    private val riskScore = Attribute(10, "Risk score", AttributeKind.DERIVED)

    private fun case(vararg attributeToValue: Pair<Attribute, String>) = with(RDRCaseBuilder()) {
        attributeToValue.forEach { addValue(it.first, defaultDate, it.second) }
        build("Case")
    }

    private val attributeFor: (String) -> Attribute? = { name ->
        setOf(weight, height, riskScore).firstOrNull { it.name == name }
    }

    private fun parseFormula(text: String): Expr =
        FormulaParser(attributeFor).parse(text) ?: error("Expected a formula: $text")

    @Test
    fun `a literal evaluates to its value`() {
        // Given a literal
        val literal = Literal("diabetic")

        // When it is evaluated against any case
        // Then the value is the literal
        literal.evaluate(case(weight to "93.0")) shouldBe "diabetic"
        literal.referencedAttributes() shouldBe emptySet()
    }

    @Test
    fun `a formula evaluates arithmetic over the latest values of attributes`() {
        // Given the BMI formula
        val bmi = Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )

        // When it is evaluated against a case with weight and height
        val value = bmi.evaluate(case(weight to "93.0", height to "2.0"))

        // Then the computed value is assigned
        value shouldBe "23.25"
    }

    @Test
    fun `a formula uses the latest value of an attribute`() {
        // Given a case with two episodes of weight
        val case = with(RDRCaseBuilder()) {
            addValue(weight, defaultDate - 1000, "90.0")
            addValue(weight, defaultDate, "100.0")
            build("Case")
        }

        // When a formula referencing weight is evaluated
        val value = Formula(Binary(Operator.PLUS, AttributeValue(weight), Num(1.0))).evaluate(case)

        // Then the latest value is used
        value shouldBe "101"
    }

    @Test
    fun `a formula referencing an attribute with no value makes no assignment`() {
        // Given the BMI formula
        val bmi = Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )

        // When it is evaluated against a case lacking height
        // Then no value results
        bmi.evaluate(case(weight to "93.0")).shouldBeNull()
    }

    @Test
    fun `a formula referencing an attribute with a non-numeric value makes no assignment`() {
        // Given a formula referencing weight
        val formula = Formula(Binary(Operator.PLUS, AttributeValue(weight), Num(1.0)))

        // When it is evaluated against a case with a non-numeric weight
        // Then no value results
        formula.evaluate(case(weight to "unknown")).shouldBeNull()
    }

    @Test
    fun `division by zero makes no assignment`() {
        // Given a formula that divides by height
        val formula = Formula(Binary(Operator.DIVIDE, AttributeValue(weight), AttributeValue(height)))

        // When it is evaluated against a case with zero height
        // Then no value results
        formula.evaluate(case(weight to "93.0", height to "0")).shouldBeNull()
    }

    @Test
    fun `whole numbers are rendered without a decimal point`() {
        // Given a formula whose result is whole
        val formula = Formula(Binary(Operator.TIMES, AttributeValue(height), Num(2.0)))

        // When it is evaluated
        // Then the value has no decimal point
        formula.evaluate(case(height to "1.5")) shouldBe "3"
    }

    @Test
    fun referencedAttributes() {
        // Given the BMI formula
        val bmi = Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )

        // Then the attributes it references are reported
        bmi.referencedAttributes() shouldBe setOf(weight, height)
    }

    @Test
    fun `formulas can reference derived attributes`() {
        // Given a formula referencing a derived attribute
        val formula = Formula(Binary(Operator.PLUS, AttributeValue(riskScore), Num(1.0)))

        // When the derived attribute has a value in the case
        val case = case(weight to "93.0").withDerivedValue(riskScore, "7")

        // Then the formula resolves against it
        formula.evaluate(case) shouldBe "8"
        formula.referencedAttributes() shouldBe setOf(riskScore)
    }

    @Test
    fun asText() {
        Literal("diabetic").asText() shouldBe "\"diabetic\""
        val bmi = Binary(
            Operator.DIVIDE,
            AttributeValue(weight),
            Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
        )
        Formula(bmi).asText() shouldBe "weight / (height * height)"
        Formula(Binary(Operator.PLUS, AttributeValue(weight), Num(1.0))).asText() shouldBe "weight + 1"
    }

    @Test
    fun serialization() {
        // Given expressions of each kind
        val literal: ValueExpression = Literal("diabetic")
        val formula: ValueExpression = Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )

        // When they are serialized and deserialized
        // Then they are unchanged
        serializeDeserialize(literal) shouldBe literal
        serializeDeserialize(formula) shouldBe formula
    }

    @Test
    fun alignAttributes() {
        // Given a formula and an id-based attribute lookup
        val formula = Formula(Binary(Operator.DIVIDE, AttributeValue(weight), AttributeValue(height)))
        val alignedWeight = Attribute(101, "weight")
        val alignedHeight = Attribute(102, "height")
        val idToAttribute: (Int) -> Attribute = { id ->
            when (id) {
                weight.id -> alignedWeight
                height.id -> alignedHeight
                else -> error("Unexpected id $id")
            }
        }

        // When the attributes are aligned
        val aligned = formula.alignAttributes(idToAttribute) as Formula

        // Then the referenced attributes are the aligned instances
        aligned.referencedAttributes().map { it.id }.toSet() shouldBe setOf(101, 102)
    }

    @Test
    fun `a by-definition expression cannot be evaluated directly`() {
        // Given the by-definition sentinel
        // When it is evaluated without having been resolved
        // Then evaluation fails: it must first be resolved to the attribute's stored definition
        shouldThrow<IllegalStateException> {
            ByDefinition.evaluate(case(weight to "93.0"))
        }.message shouldBe "A ByDefinition expression must be resolved to a concrete expression before evaluation."
    }

    @Test
    fun `a by-definition expression does not report referenced attributes directly`() {
        // Given the by-definition sentinel
        // When its referenced attributes are requested without it having been resolved
        // Then the request fails: references come from the attribute's stored definition
        shouldThrow<IllegalStateException> {
            ByDefinition.referencedAttributes()
        }.message shouldBe "A ByDefinition expression must be resolved to a concrete expression before its referenced attributes can be determined."
    }

    @Test
    fun `a by-definition expression has a fixed text form`() {
        ByDefinition.asText() shouldBe "by definition"
    }

    @Test
    fun `a by-definition expression is unchanged by attribute alignment`() {
        // Given the by-definition sentinel
        // When attributes are aligned
        // Then it is unchanged (it references no attributes itself)
        ByDefinition.alignAttributes { error("Should not be called") } shouldBe ByDefinition
    }

    @Test
    fun `a by-definition expression serializes`() {
        // Given the by-definition sentinel as a ValueExpression
        val byDefinition: ValueExpression = ByDefinition

        // When it is serialized and deserialized
        // Then it is unchanged
        serializeDeserialize(byDefinition) shouldBe ByDefinition
    }

    @Test
    fun `parsing arithmetic over attributes gives a formula`() {
        // When a formula text is parsed
        val parsed = Formula(parseFormula("weight / (height * height)"))

        // Then a formula results and it evaluates correctly
        parsed shouldBe Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )
    }

    @Test
    fun `parsing respects operator precedence`() {
        // When a mixed-precedence formula is parsed
        val parsed = Formula(parseFormula("weight + height * 2"))

        // Then multiplication binds tighter than addition
        parsed.evaluate(case(weight to "10", height to "3")) shouldBe "16"
    }

    @Test
    fun `should parse double asterisk 2 as exponentiation to the power of 2`() {
        // When a formula uses ** for exponentiation
        val parsed = Formula(parseFormula("weight / height ** 2"))

        // Then ** is interpreted as squared
        parsed.evaluate(case(weight to "93.0", height to "2.0")) shouldBe "23.25"
        parsed.referencedAttributes() shouldBe setOf(weight, height)
        parsed.asText() shouldBe "weight / height ^ 2"
    }

    @Test
    fun `should parse double asterisk 3 as exponentiation to the power of 3`() {
        // When a formula uses ** for exponentiation
        val parsed = Formula(parseFormula("weight ** 3"))

        // Then ** is interpreted as squared
        parsed.evaluate(case(weight to "2")) shouldBe "8"
        parsed.referencedAttributes() shouldBe setOf(weight)
        parsed.asText() shouldBe "weight ^ 3"
    }

    @Test
    fun `should parse caret as exponentiation to the power of 2`() {
        // When a formula uses ^ for exponentiation
        val parsed = Formula(parseFormula("weight / height ^ 2"))

        // Then ^ is interpreted as squared
        parsed.evaluate(case(weight to "93.0", height to "2.0")) shouldBe "23.25"
        parsed.referencedAttributes() shouldBe setOf(weight, height)
        parsed.asText() shouldBe "weight / height ^ 2"
    }

    @Test
    fun `should parse caret as exponentiation to the power of 3`() {
        // When a formula uses ^ for exponentiation
        val parsed = Formula(parseFormula("weight ^ 3"))

        // Then ^ is interpreted as cubed
        parsed.evaluate(case(weight to "2")) shouldBe "8"
        parsed.referencedAttributes() shouldBe setOf(weight)
        parsed.asText() shouldBe "weight ^ 3"
    }

    @Test
    fun `should ignore spaces around the caret`() {
        // When a formula uses ^ for exponentiation
        val parsed = Formula(parseFormula("weight^3"))

        // Then ^ is interpreted as cubed
        parsed.evaluate(case(weight to "2")) shouldBe "8"
        parsed.referencedAttributes() shouldBe setOf(weight)
        parsed.asText() shouldBe "weight ^ 3"
    }

    @Test
    fun `should parse float with exponentiation`() {
        // When a formula uses ^ for exponentiation
        val parsed = Formula(parseFormula("weight^3.14159"))

        // Then ^ is interpreted as cubed
        parsed.evaluate(case(weight to "2")) shouldBe "8.825"
        parsed.referencedAttributes() shouldBe setOf(weight)
        parsed.asText() shouldBe "weight ^ 3.142"
    }

    @Test
    fun `attribute names containing spaces parse in formulas`() {
        // When a formula referencing "Risk score" is parsed
        val parsed = Formula(parseFormula("Risk score + 1"))

        // Then the attribute resolves
        parsed.referencedAttributes() shouldBe setOf(riskScore)
    }

    @Test
    fun `text with an unresolvable identifier is not a formula`() {
        // When text that looks arithmetical but does not resolve is parsed
        // Then the formula parser refuses it
        FormulaParser(attributeFor).parse("gluten-free").shouldBeNull()
        FormulaParser(attributeFor).parse("B+").shouldBeNull()
    }
}
