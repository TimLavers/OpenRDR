package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AssignmentRuleTreeChangeTest {
    private val glucose = Attribute(1, "Glucose")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val diabetic = AssignValue(diabetesStatus, Literal("diabetic"))
    private val preDiabetic = AssignValue(diabetesStatus, Literal("pre-diabetic"))

    private lateinit var tree: RuleTree
    private lateinit var ruleFactory: RuleFactory
    private var nextRuleId = 100
    private var nextConditionId = 1000

    @BeforeTest
    fun setup() {
        tree = RuleTree()
        ruleFactory = object : RuleFactory {
            override fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>) =
                Rule(nextRuleId++, parent, conclusion, conditions)

            override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue, conditions: Set<Condition>) =
                Rule(nextRuleId++, parent, null, conditions, mutableSetOf(), assignment)
        }
    }

    private fun case(glucoseValue: String = "12.0") = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, glucoseValue)
        build("Fermi")
    }

    private fun highGlucose() = greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)
    private fun veryHighGlucose() = greaterThanOrEqualTo(nextConditionId++, glucose, 20.0)

    private fun buildAssignmentRule() {
        ChangeTreeToAddAssignment(diabetic).createChanger(tree, ruleFactory)
            .updateRuleTree(case(), setOf(highGlucose()))
    }

    @Test
    fun `adding an assignment is applicable only if the case does not already have it`() {
        // Given a change to add an assignment
        val change = ChangeTreeToAddAssignment(diabetic)

        // Then it is applicable to a case without the assignment
        change.isApplicable(tree, case()) shouldBe true

        // When the assignment rule is built
        buildAssignmentRule()

        // Then the change is no longer applicable
        change.isApplicable(tree, case()) shouldBe false
    }

    @Test
    fun `adding an assignment creates a rule under the root`() {
        // Given a change to add an assignment
        val change = ChangeTreeToAddAssignment(diabetic)

        // When the tree is updated
        val rulesAdded = change.createChanger(tree, ruleFactory).updateRuleTree(case(), setOf(highGlucose()))

        // Then a rule with the assignment has been added under the root
        rulesAdded shouldHaveSize 1
        with(rulesAdded.single()) {
            assignment shouldBe diabetic
            conclusion.shouldBeNull()
            parent shouldBe tree.root
        }
        tree.materialise(case()).latestValue(diabetesStatus) shouldBe "diabetic"
    }

    @Test
    fun `removing an assignment is applicable only if the case has it`() {
        // Given a tree with an assignment rule
        buildAssignmentRule()
        val change = ChangeTreeToRemoveAssignment(diabetic)

        // Then the removal is applicable only to cases with the assignment
        change.isApplicable(tree, case("12.0")) shouldBe true
        change.isApplicable(tree, case("5.0")) shouldBe false
    }

    @Test
    fun `removing an assignment adds a stopping child to the assigning rule`() {
        // Given a tree with an assignment rule
        buildAssignmentRule()

        // When a removal change is applied
        val rulesAdded = ChangeTreeToRemoveAssignment(diabetic).createChanger(tree, ruleFactory)
            .updateRuleTree(case("25.0"), setOf(veryHighGlucose()))

        // Then a stopping child was added, and the assignment is retracted
        // for cases satisfying the stopping condition only
        rulesAdded shouldHaveSize 1
        with(rulesAdded.single()) {
            assignment.shouldBeNull()
            conclusion.shouldBeNull()
            parent?.assignment shouldBe diabetic
        }
        tree.materialise(case("25.0")).latestValue(diabetesStatus).shouldBeNull()
        tree.materialise(case("12.0")).latestValue(diabetesStatus) shouldBe "diabetic"
    }

    @Test
    fun `replacing an assignment adds a child with the replacement assignment`() {
        // Given a tree with an assignment rule
        buildAssignmentRule()

        // When a replacement change is applied
        val rulesAdded = ChangeTreeToReplaceAssignment(diabetic, preDiabetic).createChanger(tree, ruleFactory)
            .updateRuleTree(case("25.0"), setOf(veryHighGlucose()))

        // Then a child with the replacement was added, and the replacement
        // applies to cases satisfying its condition only
        rulesAdded shouldHaveSize 1
        with(rulesAdded.single()) {
            assignment shouldBe preDiabetic
            parent?.assignment shouldBe diabetic
        }
        tree.materialise(case("25.0")).latestValue(diabetesStatus) shouldBe "pre-diabetic"
        tree.materialise(case("12.0")).latestValue(diabetesStatus) shouldBe "diabetic"
    }

    @Test
    fun `an assignment can only be replaced by an assignment to the same attribute`() {
        // Given assignments to different attributes
        val riskScore = Attribute(11, "Risk score", AttributeKind.DERIVED)

        // When a replacement across attributes is constructed
        // Then it is rejected
        shouldThrow<IllegalArgumentException> {
            ChangeTreeToReplaceAssignment(diabetic, AssignValue(riskScore, Literal("7")))
        }.message shouldBe "An assignment can only be replaced by an assignment to the same attribute."
    }

    @Test
    fun `alignWith leaves assignment changes unchanged`() {
        // Given assignment changes
        val add = ChangeTreeToAddAssignment(diabetic)
        val remove = ChangeTreeToRemoveAssignment(diabetic)
        val replace = ChangeTreeToReplaceAssignment(diabetic, preDiabetic)

        // When they are aligned with a conclusion factory
        // Then they are unchanged, as no conclusions are involved
        add.alignWith(DummyConclusionFactory()) shouldBe add
        remove.alignWith(DummyConclusionFactory()) shouldBe remove
        replace.alignWith(DummyConclusionFactory()) shouldBe replace
    }
}
