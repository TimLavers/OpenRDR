package io.rippledown.model.rule

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isCondition
import io.rippledown.model.condition.isPresent
import io.rippledown.utils.defaultDate
import kotlin.test.Test

internal class RuleTreeFixpointTest {
    private val glucose = Attribute(1, "Glucose")
    private val weight = Attribute(2, "weight")
    private val height = Attribute(3, "height")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val riskScore = Attribute(11, "Risk score", AttributeKind.DERIVED)
    private val bmi = Attribute(12, "BMI", AttributeKind.DERIVED)
    private val advice = Conclusion(1, "Diabetic diet advice given.")

    private var nextRuleId = 100
    private var nextConditionId = 1000

    private fun case(vararg attributeToValue: Pair<Attribute, String>) = with(RDRCaseBuilder()) {
        attributeToValue.forEach { addValue(it.first, defaultDate, it.second) }
        build("Fermi")
    }

    private fun RuleTree.withRule(
        conclusion: Conclusion? = null,
        assignment: AssignValue? = null,
        vararg conditions: io.rippledown.model.condition.Condition
    ): Rule {
        val rule = Rule(nextRuleId++, null, conclusion, conditions.toSet(), mutableSetOf(), assignment)
        root.addChild(rule)
        return rule
    }

    private fun bmiFormula() = Formula(
        Binary(
            Operator.DIVIDE,
            AttributeValue(weight),
            Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
        )
    )

    @Test
    fun `an assignment rule writes its value onto the case`() {
        // Given a tree with a rule assigning a derived value
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then the derived value is on the materialised case
        materialised.latestValue(diabetesStatus) shouldBe "diabetic"
        case.interpretation.assignments().map { it.attribute } shouldBe listOf(diabetesStatus)
    }

    @Test
    fun `an assignment rule whose conditions do not hold assigns nothing`() {
        // Given a tree with a rule assigning a derived value for high glucose
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val case = case(glucose to "5.0")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then no derived value is assigned
        materialised.latestValue(diabetesStatus).shouldBeNull()
        case.interpretation.assignments() shouldBe emptySet()
    }

    @Test
    fun `a rule conditioned on a derived value fires on a later pass`() {
        // Given a rule assigning a derived value and a rule conditioned on it
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        tree.withRule(
            conclusion = advice,
            conditions = arrayOf(isCondition(nextConditionId++, diabetesStatus, "diabetic"))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the dependent conclusion is given
        interpretation.conclusions() shouldBe setOf(advice)
    }

    @Test
    fun `a chain of derived attributes resolves across passes`() {
        // Given a chain: glucose -> status -> risk score -> conclusion
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        tree.withRule(
            assignment = AssignValue(riskScore, Literal("7")),
            conditions = arrayOf(isCondition(nextConditionId++, diabetesStatus, "diabetic"))
        )
        tree.withRule(
            conclusion = advice,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, riskScore, 6.0))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then the whole chain has fired
        materialised.latestValue(diabetesStatus) shouldBe "diabetic"
        materialised.latestValue(riskScore) shouldBe "7"
        case.interpretation.conclusions() shouldBe setOf(advice)
    }

    @Test
    fun `a rule conditioned on the absence of a derived attribute fires when it is not assigned`() {
        // Given an assignment rule that does not fire, and a rule conditioned on the absence
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val noEvidence = Conclusion(2, "No evidence of diabetes.")
        tree.withRule(
            conclusion = noEvidence,
            conditions = arrayOf(isAbsent(diabetesStatus, nextConditionId++))
        )
        val case = case(glucose to "5.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the absence-conditioned rule has fired
        interpretation.conclusions() shouldBe setOf(noEvidence)
    }

    @Test
    fun `a rule conditioned on the absence of a derived attribute does not fire when it is assigned`() {
        // Given an assignment rule that fires, and a rule conditioned on the absence
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val noEvidence = Conclusion(2, "No evidence of diabetes.")
        tree.withRule(
            conclusion = noEvidence,
            conditions = arrayOf(isAbsent(diabetesStatus, nextConditionId++))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the absence-conditioned rule has been retracted on the later pass
        interpretation.conclusions() shouldBe emptySet()
    }

    @Test
    fun `an unconditional formula rule assigns a computed value usable in conditions`() {
        // Given an unconditional BMI formula rule and a dependent rule
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, bmiFormula()))
        val elevated = Conclusion(3, "Elevated BMI.")
        tree.withRule(
            conclusion = elevated,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, bmi, 28.0))
        )
        val case = case(weight to "93.0", height to "1.8")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then the computed value is assigned and the dependent rule fired
        materialised.latestValue(bmi) shouldBe "28.7"
        case.interpretation.conclusions() shouldBe setOf(elevated)
    }

    @Test
    fun `a formula referencing an attribute with no value makes no assignment`() {
        // Given an unconditional BMI formula rule
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, bmiFormula()))
        val case = case(weight to "93.0")

        // When a case lacking height is interpreted
        val materialised = tree.materialise(case)

        // Then no BMI is assigned
        materialised.latestValue(bmi).shouldBeNull()
        materialised.attributes.contains(bmi) shouldBe false
    }

    @Test
    fun `a formula is overridden by a conditioned child rule`() {
        // Given a formula rule with a child giving a corrected formula
        val tree = RuleTree()
        val formulaRule = tree.withRule(assignment = AssignValue(bmi, bmiFormula()))
        val corrected = AssignValue(bmi, Formula(Binary(Operator.TIMES, AttributeValue(weight), Num(2.0))))
        val child = Rule(
            nextRuleId++, null, null,
            setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)), mutableSetOf(), corrected
        )
        formulaRule.addChild(child)

        // When a case satisfying the child's condition is interpreted
        val materialised = tree.materialise(case(weight to "93.0", height to "1.8", glucose to "12.0"))

        // Then the leaf-most rule's formula wins
        materialised.latestValue(bmi) shouldBe "186"
    }

    @Test
    fun `interpretation is idempotent`() {
        // Given a tree with chained assignment rules
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        tree.withRule(
            conclusion = advice,
            conditions = arrayOf(isCondition(nextConditionId++, diabetesStatus, "diabetic"))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted repeatedly, including from a materialised copy
        val first = tree.materialise(case)
        val second = tree.materialise(first)
        val third = tree.materialise(second)

        // Then the result is stable
        second.hasSameDataAs(first) shouldBe true
        third.hasSameDataAs(first) shouldBe true
        case.interpretation.conclusions() shouldBe setOf(advice)
    }

    @Test
    fun `stale derived values from a previous interpretation are stripped`() {
        // Given a case carrying a derived value that no rule assigns
        val tree = RuleTree()
        val withStale = case(glucose to "5.0").withDerivedValue(diabetesStatus, "diabetic")

        // When the case is interpreted
        val materialised = tree.materialise(withStale)

        // Then the stale value is gone
        materialised.latestValue(diabetesStatus).shouldBeNull()
        materialised.attributes.contains(diabetesStatus) shouldBe false
    }

    @Test
    fun `a stopping child retracts an assignment`() {
        // Given an assignment rule with a stopping child rule
        val tree = RuleTree()
        val assigning = tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val stopper = Rule(
            nextRuleId++, null, null,
            setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 20.0)), mutableSetOf(), null
        )
        assigning.addChild(stopper)

        // When a case satisfying the stopping condition is interpreted
        val materialised = tree.materialise(case(glucose to "25.0"))

        // Then no assignment is made
        materialised.latestValue(diabetesStatus).shouldBeNull()
    }

    @Test
    fun `a retraction on a later pass leaves no stale value`() {
        // Given a rule assigning riskScore, and a stopping child of that rule
        // conditioned on a derived value that itself is assigned on the first pass
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val scoring = tree.withRule(assignment = AssignValue(riskScore, Literal("7")))
        val stopper = Rule(
            nextRuleId++, null, null,
            setOf(isPresent(diabetesStatus, nextConditionId++)), mutableSetOf(), null
        )
        scoring.addChild(stopper)

        // When the case is interpreted
        // Pass 1: both assignments fire (diabetes status not yet on the case).
        // Pass 2: the stopper fires, retracting the risk score.
        val materialised = tree.materialise(case(glucose to "12.0"))

        // Then the retracted assignment has left no stale value
        materialised.latestValue(riskScore).shouldBeNull()
        materialised.latestValue(diabetesStatus) shouldBe "diabetic"
    }

    @Test
    fun `interpreting a case with no episodes makes no assignments`() {
        // Given an unconditional assignment rule and an empty case
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(diabetesStatus, Literal("diabetic")))
        val empty = RDRCaseBuilder().build("Empty")

        // When the case is interpreted
        val materialised: RDRCase = tree.materialise(empty)

        // Then no assignment is made and no error is thrown
        materialised.numberOfEpisodes() shouldBe 0
    }

    @Test
    fun `conclusion-only interpretation behaves as a single pass`() {
        // Given a tree with only conclusion rules
        val tree = RuleTree()
        tree.withRule(
            conclusion = advice,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the conclusion is given and the case data is unchanged
        interpretation.conclusions() shouldBe setOf(advice)
        tree.materialise(case).hasSameDataAs(case) shouldBe true
    }
}
