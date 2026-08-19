package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.CommentFactory
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isCondition
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionDerivedValuesTest {
    private val glucose = Attribute(1, "Glucose")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val commentFactory = CommentFactory()
    private val advice = commentFactory.comment("Diabetic diet advice given.")
    private val ruleFactory = DummyRuleFactory()

    private lateinit var tree: RuleTree

    private fun case(name: String, glucoseValue: String) = with(RDRCaseBuilder()) {
        addValue(glucose, defaultDate, glucoseValue)
        build(name)
    }

    @BeforeTest
    fun setup() {
        // A tree with a rule assigning a derived value for high glucose.
        tree = RuleTree()
        val assigning = Rule(
            1, null,
            setOf(greaterThanOrEqualTo(100, glucose, 11.0)), mutableSetOf(),
            AssignValue(diabetesStatus, Literal("diabetic"))
        )
        tree.root.addChild(assigning)
    }

    @Test
    fun `a condition on a derived value assigned by the tree can be added`() {
        // Given a session for a case whose derived value is assigned by the tree
        val case = case("A", "12.0")
        val session = RuleBuildingSession(ruleFactory, tree, case, ChangeTreeToAddAssignment(advice), listOf())

        // When a condition on the derived value is added
        session.addCondition(isCondition(200, diabetesStatus, "diabetic"))

        // Then it is accepted
        session.conditions shouldBe mutableSetOf(isCondition(200, diabetesStatus, "diabetic"))
    }

    @Test
    fun `a condition on a derived value not assigned for the case is rejected`() {
        // Given a session for a case whose glucose is too low for the assignment rule
        val case = case("A", "5.0")
        val session = RuleBuildingSession(ruleFactory, tree, case, ChangeTreeToAddAssignment(advice), listOf())

        // When a condition on the derived value is added
        // Then it is rejected
        shouldThrow<IllegalArgumentException> {
            session.addCondition(isCondition(200, diabetesStatus, "diabetic"))
        }
    }

    @Test
    fun `a condition on a by-definition derived value can be added when a resolver is supplied`() {
        // Given a tree whose assignment rule gives the derived value by definition
        tree = RuleTree()
        val assigning = Rule(
            1, null,
            setOf(greaterThanOrEqualTo(100, glucose, 11.0)), mutableSetOf(),
            AssignValue(diabetesStatus, ByDefinition)
        )
        tree.root.addChild(assigning)
        val resolver: DefinitionResolver = { attribute ->
            if (attribute == diabetesStatus) Literal("diabetic") else null
        }

        // When a session with the resolver adds a condition on the derived value
        val case = case("A", "12.0")
        val session =
            RuleBuildingSession(ruleFactory, tree, case, ChangeTreeToAddAssignment(advice), listOf(), resolver)
        session.addCondition(isCondition(200, diabetesStatus, "diabetic"))

        // Then it is accepted
        session.conditions shouldBe mutableSetOf(isCondition(200, diabetesStatus, "diabetic"))
    }

    @Test
    fun `conditions on derived values are evaluated against materialised cornerstones`() {
        // Given a session with two conflicting cornerstones, one of which
        // is assigned the derived value by the tree
        val case = case("A", "12.0")
        val diabeticCornerstone = case("B", "15.0")
        val nonDiabeticCornerstone = case("C", "5.0")
        val session = RuleBuildingSession(
            ruleFactory, tree, case, ChangeTreeToAddAssignment(advice),
            listOf(diabeticCornerstone, nonDiabeticCornerstone)
        )
        session.cornerstoneCases().map { it.name } shouldBe listOf("B", "C")

        // When a condition on the derived value is added
        session.addCondition(isCondition(200, diabetesStatus, "diabetic"))

        // Then only the cornerstone with the derived value remains conflicting
        session.cornerstoneCases().map { it.name } shouldBe listOf("B")
    }
}
