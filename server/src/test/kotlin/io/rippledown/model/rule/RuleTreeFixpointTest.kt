package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isCondition
import io.rippledown.model.condition.isPresent
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.AiEvaluationResult
import io.rippledown.model.rule.evaluation.DefaultValueExpressionEvaluator
import io.rippledown.model.rule.evaluation.EvaluationContext
import io.rippledown.model.rule.evaluation.EvaluationOutcome
import io.rippledown.model.rule.evaluation.ValueExpressionEvaluator
import io.rippledown.utils.defaultDate
import kotlin.test.Test

internal class RuleTreeFixpointTest {
    private val glucose = Attribute(1, "Glucose")
    private val weight = Attribute(2, "weight")
    private val height = Attribute(3, "height")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val riskScore = Attribute(11, "Risk score", AttributeKind.DERIVED)
    private val bmi = Attribute(12, "BMI", AttributeKind.DERIVED)
    private val advice =
        AssignValue(Attribute(20, "C1", AttributeKind.COMMENT), CommentTemplate("Diabetic diet advice given."))

    private var nextRuleId = 100
    private var nextConditionId = 1000

    private fun case(vararg attributeToValue: Pair<Attribute, String>) = with(RDRCaseBuilder()) {
        attributeToValue.forEach { addValue(it.first, defaultDate, it.second) }
        build("Fermi")
    }

    private fun RuleTree.withRule(
        assignment: AssignValue? = null,
        vararg conditions: io.rippledown.model.condition.Condition
    ): Rule {
        val rule = Rule(nextRuleId++, null, conditions.toSet(), mutableSetOf(), assignment)
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
            assignment = advice,
            conditions = arrayOf(isCondition(nextConditionId++, diabetesStatus, "diabetic"))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the dependent comment is given, along with the derived value it
        // is conditioned on: a comment is an assignment like any other.
        interpretation.assignments() shouldBe setOf(AssignValue(diabetesStatus, Literal("diabetic")), advice)
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
            assignment = advice,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, riskScore, 6.0))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then the whole chain has fired
        materialised.latestValue(diabetesStatus) shouldBe "diabetic"
        materialised.latestValue(riskScore) shouldBe "7"
        case.interpretation.assignments() shouldBe setOf(
            AssignValue(diabetesStatus, Literal("diabetic")),
            AssignValue(riskScore, Literal("7")),
            advice
        )
    }

    @Test
    fun `a rule conditioned on the absence of a derived attribute fires when it is not assigned`() {
        // Given an assignment rule that does not fire, and a rule conditioned on the absence
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val noEvidence =
            AssignValue(Attribute(21, "C2", AttributeKind.COMMENT), CommentTemplate("No evidence of diabetes."))
        tree.withRule(
            assignment = noEvidence,
            conditions = arrayOf(isAbsent(diabetesStatus, nextConditionId++))
        )
        val case = case(glucose to "5.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the absence-conditioned rule has fired
        interpretation.assignments() shouldBe setOf(noEvidence)
    }

    @Test
    fun `a rule conditioned on the absence of a derived attribute does not fire when it is assigned`() {
        // Given an assignment rule that fires, and a rule conditioned on the absence
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(diabetesStatus, Literal("diabetic")),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val noEvidence =
            AssignValue(Attribute(21, "C2", AttributeKind.COMMENT), CommentTemplate("No evidence of diabetes."))
        tree.withRule(
            assignment = noEvidence,
            conditions = arrayOf(isAbsent(diabetesStatus, nextConditionId++))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the absence-conditioned comment has been retracted on the later
        // pass, leaving only the derived value that was assigned
        interpretation.assignments() shouldBe setOf(AssignValue(diabetesStatus, Literal("diabetic")))
    }

    @Test
    fun `an unconditional formula rule assigns a computed value usable in conditions`() {
        // Given an unconditional BMI formula rule and a dependent rule
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, bmiFormula()))
        val elevated = AssignValue(Attribute(22, "C3", AttributeKind.COMMENT), CommentTemplate("Elevated BMI."))
        tree.withRule(
            assignment = elevated,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, bmi, 28.0))
        )
        val case = case(weight to "93.0", height to "1.8")

        // When the case is interpreted
        val materialised = tree.materialise(case)

        // Then the computed value is assigned and the dependent rule fired
        materialised.latestValue(bmi) shouldBe "28.7"
        case.interpretation.assignments() shouldBe setOf(AssignValue(bmi, bmiFormula()), elevated)
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
            nextRuleId++, null, setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)), mutableSetOf(), corrected
        )
        formulaRule.addChild(child)

        // When a case satisfying the child's condition is interpreted
        val materialised = tree.materialise(case(weight to "93.0", height to "1.8", glucose to "12.0"))

        // Then the leaf-most rule's formula wins
        materialised.latestValue(bmi) shouldBe "186"
    }

    @Test
    fun `a by-definition assignment resolves against the stored definition`() {
        // Given an unconditional rule assigning BMI by definition, and a resolver holding the BMI formula
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, ByDefinition))
        val resolver: DefinitionResolver = { attribute -> if (attribute == bmi) bmiFormula() else null }

        // When the case is materialised with the resolver
        val materialised = tree.materialise(case(weight to "93.0", height to "1.8"), resolver)

        // Then the definition's value is assigned
        materialised.latestValue(bmi) shouldBe "28.7"
    }

    @Test
    fun `a by-definition assignment with no stored definition assigns nothing`() {
        // Given a rule assigning BMI by definition, but no definition for it
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, ByDefinition))

        // When the case is materialised with a resolver that has no definition
        val materialised = tree.materialise(case(weight to "93.0", height to "1.8"), resolver = { null })

        // Then no assignment is made and no error is thrown
        materialised.latestValue(bmi).shouldBeNull()
        materialised.attributes.contains(bmi) shouldBe false
    }

    @Test
    fun `editing the definition changes the assigned value on re-interpretation`() {
        // Given a rule assigning BMI by definition
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, ByDefinition))
        val case = case(weight to "93.0", height to "1.8")

        // When the case is materialised before and after the definition is edited
        val before = tree.materialise(case, resolver = { bmiFormula() })
        val editedDefinition = Formula(Binary(Operator.TIMES, AttributeValue(weight), Num(2.0)))
        val after = tree.materialise(case, resolver = { editedDefinition })

        // Then the value reflects the edited definition, with no rule change
        before.latestValue(bmi) shouldBe "28.7"
        after.latestValue(bmi) shouldBe "186"
    }

    @Test
    fun `a concrete override in a child rule beats the by-definition parent`() {
        // Given a by-definition rule with a conditioned child carrying a concrete override
        val tree = RuleTree()
        val byDefinitionRule = tree.withRule(assignment = AssignValue(bmi, ByDefinition))
        val override = AssignValue(bmi, Formula(Binary(Operator.TIMES, AttributeValue(weight), Num(2.0))))
        val child = Rule(
            nextRuleId++, null, setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)), mutableSetOf(), override
        )
        byDefinitionRule.addChild(child)

        // When a case satisfying the child's condition is materialised
        val materialised =
            tree.materialise(case(weight to "93.0", height to "1.8", glucose to "12.0"), resolver = { bmiFormula() })

        // Then the leaf-most rule's concrete expression wins over the definition
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
            assignment = advice,
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
        case.interpretation.assignments() shouldBe setOf(AssignValue(diabetesStatus, Literal("diabetic")), advice)
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
            nextRuleId++, null, setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 20.0)), mutableSetOf(), null
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
            nextRuleId++, null, setOf(isPresent(diabetesStatus, nextConditionId++)), mutableSetOf(), null
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

    private val notes = Attribute(4, "clinical_notes")
    private val hasDiabetes = Attribute(13, "hasDiabetes", AttributeKind.DERIVED)
    private val hasDiabetesDefinition = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm a personal history of diabetes.",
        revision = "rev-1",
        evaluationProfile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    )
    private val aiResolver: DefinitionResolver = { if (it == hasDiabetes) hasDiabetesDefinition else null }
    private val diabetic = AssignValue(Attribute(23, "C4", AttributeKind.COMMENT), CommentTemplate("Diabetic."))
    private val notDiabetic = AssignValue(Attribute(24, "C5", AttributeKind.COMMENT), CommentTemplate("Not diabetic."))
    private val unknown = AssignValue(Attribute(25, "C6", AttributeKind.COMMENT), CommentTemplate("Diabetes status unknown."))

    private class CountingEvaluator : ValueExpressionEvaluator {
        private val delegate = DefaultValueExpressionEvaluator()
        val evaluated = mutableListOf<ValueExpression>()
        override fun evaluate(expression: ValueExpression, case: RDRCase, context: EvaluationContext): EvaluationOutcome {
            evaluated.add(expression)
            return delegate.evaluate(expression, case, context)
        }
        fun aiEvaluations() = evaluated.count { it is AiBoolean }
    }

    private fun success(value: Boolean, evidence: String? = null) =
        AiEvaluationResult.Success(value, evidence, 1_700_000_000_000, "gemini-2.5-flash-001")

    private fun contextWith(input: String, result: AiEvaluationResult) =
        EvaluationContext.EMPTY.with(AiEvaluationRequest(hasDiabetesDefinition, input), result)

    private fun RuleTree.withDiabetesRules(): RuleTree {
        withRule(assignment = AssignValue(hasDiabetes, ByDefinition))
        withRule(assignment = diabetic, conditions = arrayOf(isCondition(nextConditionId++, hasDiabetes, "true")))
        withRule(assignment = notDiabetic, conditions = arrayOf(isCondition(nextConditionId++, hasDiabetes, "false")))
        withRule(assignment = unknown, conditions = arrayOf(isAbsent(hasDiabetes, nextConditionId++)))
        return this
    }

    @Test
    fun `a prepared true result is materialised and drives the downstream rule`() {
        // Given rules on an AI Boolean, and a prepared true result for the case's notes
        val tree = RuleTree().withDiabetesRules()
        val case = case(notes to "T2 Diab")
        val context = contextWith("T2 Diab", success(true, "T2 Diab"))
        val evaluator = CountingEvaluator()

        // When the case is inferred
        val outcome = tree.infer(case, aiResolver, evaluator, context)

        // Then the value is on the case, only the true rule fired, and the provenance is reported
        val complete = outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        complete.case.latestValue(hasDiabetes) shouldBe "true"
        complete.case.interpretation.assignments() shouldBe setOf(AssignValue(hasDiabetes, ByDefinition), diabetic)
        complete.provenance shouldBe mapOf(
            hasDiabetes to AiProvenance(
                sourceName = "clinical_notes",
                instructions = hasDiabetesDefinition.instructions,
                evidence = "T2 Diab",
                evaluatedAt = 1_700_000_000_000,
                model = "gemini-2.5-flash-001",
                revision = "rev-1"
            )
        )
    }

    @Test
    fun `a prepared false result is a value, not an absence`() {
        // Given a prepared false result
        val tree = RuleTree().withDiabetesRules()
        val case = case(notes to "FH diab")
        val context = contextWith("FH diab", success(false))

        // When the case is inferred
        val outcome = tree.infer(case, aiResolver, CountingEvaluator(), context)

        // Then the false rule fired and the absence rule did not
        val complete = outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        complete.case.latestValue(hasDiabetes) shouldBe "false"
        complete.case.interpretation.assignments() shouldBe setOf(AssignValue(hasDiabetes, ByDefinition), notDiabetic)
    }

    @Test
    fun `the prepared result is reused on every pass`() {
        // Given a chain that needs several passes to stabilise
        val tree = RuleTree().withDiabetesRules()
        val case = case(notes to "T2 Diab")
        val evaluator = CountingEvaluator()

        // When the case is inferred
        tree.infer(case, aiResolver, evaluator, contextWith("T2 Diab", success(true)))

        // Then the AI expression was evaluated once per pass (assign, fire downstream, confirm stable), with no demand
        evaluator.aiEvaluations() shouldBe 3
    }

    @Test
    fun `a missing result ends the attempt with a demand and no partial interpretation`() {
        // Given no prepared result for the case's notes
        val tree = RuleTree().withDiabetesRules()
        val case = case(notes to "T2 Diab")

        // When the case is inferred
        val outcome = tree.infer(case, aiResolver, CountingEvaluator(), EvaluationContext.EMPTY)

        // Then the demand names the exact request, and nothing has been concluded
        outcome shouldBe InferenceOutcome.Demand(hasDiabetes, AiEvaluationRequest(hasDiabetesDefinition, "T2 Diab"))
        case.interpretation.assignments() shouldBe emptySet()
    }

    @Test
    fun `a failed evaluation ends the attempt without firing absence rules`() {
        // Given a prepared failure for the case's notes
        val tree = RuleTree().withDiabetesRules()
        val case = case(notes to "T2 Diab")
        val context = contextWith("T2 Diab", AiEvaluationResult.Failure("Gemini unavailable"))

        // When the case is inferred
        val outcome = tree.infer(case, aiResolver, CountingEvaluator(), context)

        // Then the outcome is incomplete, naming the attribute, and the absence rule has not fired
        outcome shouldBe InferenceOutcome.Incomplete(hasDiabetes, "Gemini unavailable")
        case.interpretation.assignments() shouldBe emptySet()
    }

    @Test
    fun `a case without the source value needs no evaluation and lets absence rules fire`() {
        // Given a case with no notes
        val tree = RuleTree().withDiabetesRules()
        val case = case(glucose to "5.0")

        // When the case is inferred with no prepared results
        val outcome = tree.infer(case, aiResolver, CountingEvaluator(), EvaluationContext.EMPTY)

        // Then inference completes with the absence rule fired and no provenance
        val complete = outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        complete.case.latestValue(hasDiabetes).shouldBeNull()
        complete.case.interpretation.assignments() shouldBe setOf(AssignValue(hasDiabetes, ByDefinition), unknown)
        complete.provenance shouldBe emptyMap()
    }

    @Test
    fun `a blank source value is no input`() {
        val tree = RuleTree().withDiabetesRules()

        val outcome = tree.infer(case(notes to "   "), aiResolver, CountingEvaluator(), EvaluationContext.EMPTY)

        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>().case.interpretation.assignments() shouldBe
                setOf(AssignValue(hasDiabetes, ByDefinition), unknown)
    }

    @Test
    fun `an inactive AI rule is not evaluated`() {
        // Given an AI assignment rule whose condition does not hold
        val tree = RuleTree()
        tree.withRule(
            assignment = AssignValue(hasDiabetes, ByDefinition),
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val evaluator = CountingEvaluator()

        // When a case not satisfying the condition is inferred with no prepared results
        val outcome = tree.infer(case(notes to "T2 Diab", glucose to "5.0"), aiResolver, evaluator, EvaluationContext.EMPTY)

        // Then inference completes without any demand
        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        evaluator.aiEvaluations() shouldBe 0
    }

    @Test
    fun `a retracted AI assignment is not evaluated`() {
        // Given an AI assignment rule with a stopping child whose condition holds
        val tree = RuleTree()
        val assigning = tree.withRule(assignment = AssignValue(hasDiabetes, ByDefinition))
        assigning.addChild(
            Rule(nextRuleId++, null, setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)), mutableSetOf(), null)
        )
        val evaluator = CountingEvaluator()

        // When the case is inferred with no prepared results
        val outcome = tree.infer(case(notes to "T2 Diab", glucose to "12.0"), aiResolver, evaluator, EvaluationContext.EMPTY)

        // Then no demand is made
        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        evaluator.aiEvaluations() shouldBe 0
    }

    @Test
    fun `a literal override in a child rule is not evaluated as AI`() {
        // Given an AI by-definition rule with a conditioned child overriding it with a literal
        val tree = RuleTree()
        val byDefinition = tree.withRule(assignment = AssignValue(hasDiabetes, ByDefinition))
        byDefinition.addChild(
            Rule(
                nextRuleId++, null, setOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)), mutableSetOf(),
                AssignValue(hasDiabetes, Literal("true"))
            )
        )
        val evaluator = CountingEvaluator()

        // When a case satisfying the child's condition is inferred with no prepared results
        val outcome = tree.infer(case(notes to "FH diab", glucose to "12.0"), aiResolver, evaluator, EvaluationContext.EMPTY)

        // Then the literal is assigned and no classification was needed
        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>().case.latestValue(hasDiabetes) shouldBe "true"
        evaluator.aiEvaluations() shouldBe 0
    }

    @Test
    fun `an AI value activating a rule on a later pass still needs no new request`() {
        // Given a rule whose firing depends on the AI value and itself assigns a derived value
        val tree = RuleTree().withDiabetesRules()
        tree.withRule(
            assignment = AssignValue(riskScore, Literal("7")),
            conditions = arrayOf(isCondition(nextConditionId++, hasDiabetes, "true"))
        )
        val case = case(notes to "T2 Diab")

        // When the case is inferred with the one prepared result
        val outcome = tree.infer(case, aiResolver, CountingEvaluator(), contextWith("T2 Diab", success(true)))

        // Then the later-pass rule has fired
        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>().case.latestValue(riskScore) shouldBe "7"
    }

    @Test
    fun `a stale AI value is stripped when the source is gone`() {
        // Given a case carrying a derived AI value but no longer the notes
        val tree = RuleTree().withDiabetesRules()
        val stale = case(glucose to "5.0").withDerivedValue(hasDiabetes, "true")

        // When inferred
        val outcome = tree.infer(stale, aiResolver, CountingEvaluator(), EvaluationContext.EMPTY)

        // Then the value is gone and the absence rule fires
        val complete = outcome.shouldBeInstanceOf<InferenceOutcome.Complete>()
        complete.case.attributes.contains(hasDiabetes) shouldBe false
        complete.case.interpretation.assignments() shouldBe setOf(AssignValue(hasDiabetes, ByDefinition), unknown)
    }

    @Test
    fun `different notes are different requests`() {
        // Given a result prepared for other notes
        val tree = RuleTree().withDiabetesRules()
        val context = contextWith("FH diab", success(false))

        // When a case with different notes is inferred
        val outcome = tree.infer(case(notes to "T2 Diab"), aiResolver, CountingEvaluator(), context)

        // Then a demand for the new notes is made
        outcome shouldBe InferenceOutcome.Demand(hasDiabetes, AiEvaluationRequest(hasDiabetesDefinition, "T2 Diab"))
    }

    @Test
    fun `local-only interpretation is unchanged by the evaluator`() {
        // Given the local rules of the earlier tests
        val tree = RuleTree()
        tree.withRule(assignment = AssignValue(bmi, ByDefinition))

        // When inferred with the default evaluator and an empty context
        val outcome = tree.infer(case(weight to "93.0", height to "1.8"), { bmiFormula() })

        // Then the formula is evaluated locally
        outcome.shouldBeInstanceOf<InferenceOutcome.Complete>().case.latestValue(bmi) shouldBe "28.7"
    }

    @Test
    fun `the legacy entry points refuse an unprepared AI evaluation`() {
        // Given rules on an AI Boolean and a case needing a classification
        val tree = RuleTree().withDiabetesRules()

        // When the synchronous entry points are used
        val fromMaterialise = shouldThrow<IncompleteInferenceException> { tree.materialise(case(notes to "T2 Diab"), aiResolver) }
        val fromApply = shouldThrow<IncompleteInferenceException> { tree.apply(case(notes to "T2 Diab"), aiResolver) }

        // Then they fail rather than publish a partial interpretation
        fromMaterialise.outcome shouldBe InferenceOutcome.Demand(hasDiabetes, AiEvaluationRequest(hasDiabetesDefinition, "T2 Diab"))
        fromApply.message shouldContain "hasDiabetes"
    }

    @Test
    fun `stopped outcomes describe themselves by attribute`() {
        InferenceOutcome.Demand(hasDiabetes, AiEvaluationRequest(hasDiabetesDefinition, "T2 Diab")).description shouldBe
                "Interpretation needs an AI evaluation of hasDiabetes that has not been prepared."
        InferenceOutcome.Incomplete(hasDiabetes, "Gemini unavailable").description shouldBe
                "The AI evaluation of hasDiabetes failed: Gemini unavailable"
    }

    @Test
    fun `assignment-only interpretation behaves as a single pass`() {
        // Given a tree with only assignment rules
        val tree = RuleTree()
        tree.withRule(
            assignment = advice,
            conditions = arrayOf(greaterThanOrEqualTo(nextConditionId++, glucose, 11.0))
        )
        val case = case(glucose to "12.0")

        // When the case is interpreted
        val interpretation = tree.apply(case)

        // Then the assignment is made, and the comment's value is written onto
        // the case, which is stable under further passes.
        interpretation.assignments() shouldBe setOf(advice)
        val materialised = tree.materialise(case)
        materialised.latestValue(advice.attribute) shouldBe "Diabetic diet advice given."
        tree.materialise(materialised).hasSameDataAs(materialised) shouldBe true
    }
}
