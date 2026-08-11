package io.rippledown.model.rule

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.Conclusion
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isAbsent
import io.rippledown.model.condition.isPresent
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DerivedAttributeDependencyGraphTest {
    private val glucose = Attribute(1, "Glucose")
    private val a = Attribute(10, "A", AttributeKind.DERIVED)
    private val b = Attribute(11, "B", AttributeKind.DERIVED)
    private val c = Attribute(12, "C", AttributeKind.DERIVED)

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

    private fun graph() = DerivedAttributeDependencyGraph(tree, setOf(glucose, a, b, c))

    private fun assignmentRule(assigned: Attribute, vararg conditions: Condition): Rule {
        val rule = ruleFactory.createRuleAndAddToParent(
            tree.root,
            AssignValue(assigned, Literal("value")),
            conditions.toSet()
        )
        tree.root.addChild(rule)
        return rule
    }

    private fun highGlucose() = greaterThanOrEqualTo(nextConditionId++, glucose, 11.0)

    @Test
    fun `no cycle in an empty tree`() {
        graph().cycleCreatedBy(a, setOf(b)).shouldBeNull()
    }

    @Test
    fun `no cycle for conditions on external attributes`() {
        assignmentRule(a, isPresent(glucose, nextConditionId++))
        graph().cycleCreatedBy(a, setOf()).shouldBeNull()
        graph().cycleCreatedBy(ChangeTreeToAddAssignment(AssignValue(a, Literal("x"))), highGlucose()).shouldBeNull()
    }

    @Test
    fun `a direct self reference is a cycle`() {
        graph().cycleCreatedBy(a, setOf(a)) shouldBe listOf(a, a)
    }

    @Test
    fun `a two attribute cycle is detected`() {
        // Given a rule that assigns A with a condition on B
        assignmentRule(a, isAbsent(b, nextConditionId++))

        // Then an assignment of B conditioned on A would create a cycle
        graph().cycleCreatedBy(b, setOf(a)) shouldBe listOf(b, a, b)

        // And an assignment of C conditioned on A would not
        graph().cycleCreatedBy(c, setOf(a)).shouldBeNull()
    }

    @Test
    fun `a three attribute cycle is detected`() {
        // Given A depends on B and B depends on C
        assignmentRule(a, isPresent(b, nextConditionId++))
        assignmentRule(b, isPresent(c, nextConditionId++))

        // Then an assignment of C conditioned on A would create a cycle
        graph().cycleCreatedBy(c, setOf(a)) shouldBe listOf(c, a, b, c)
    }

    @Test
    fun `conditions of ancestor rules create dependencies`() {
        // Given an assignment of B by a child of a rule with a condition on A
        val parent = ruleFactory.createRuleAndAddToParent(
            tree.root, null as Conclusion?, setOf(isPresent(a, nextConditionId++))
        )
        tree.root.addChild(parent)
        val child = ruleFactory.createRuleAndAddToParent(parent, AssignValue(b, Literal("x")), emptySet())
        parent.addChild(child)

        // Then an assignment of A conditioned on B would create a cycle
        graph().cycleCreatedBy(a, setOf(b)) shouldBe listOf(a, b, a)
    }

    @Test
    fun `a stopping child of an assignment rule creates dependencies for the stopped attribute`() {
        // Given an assignment of A with a stopping child conditioned on B
        val assigning = assignmentRule(a, highGlucose())
        val stopper = ruleFactory.createRuleAndAddToParent(
            assigning, null as Conclusion?, setOf(isPresent(b, nextConditionId++))
        )
        assigning.addChild(stopper)

        // Then an assignment of B conditioned on A would create a cycle
        graph().cycleCreatedBy(b, setOf(a)) shouldBe listOf(b, a, b)
    }

    @Test
    fun `value expressions create dependencies`() {
        // Given a rule whose expression for A references B
        val rule = ruleFactory.createRuleAndAddToParent(
            tree.root, AssignValue(a, Formula(AttributeValue(b))), emptySet()
        )
        tree.root.addChild(rule)

        // Then an assignment of B conditioned on A would create a cycle
        graph().cycleCreatedBy(b, setOf(a)) shouldBe listOf(b, a, b)
    }

    @Test
    fun `an action expression referencing its own attribute is a cycle`() {
        // Given an action assigning A the value A * 2
        val action = ChangeTreeToAddAssignment(
            AssignValue(a, Formula(Binary(Operator.TIMES, AttributeValue(a), Num(2.0))))
        )

        // Then the action alone creates a cycle
        graph().cycleCreatedBy(action, null) shouldBe listOf(a, a)
    }

    @Test
    fun `actions that do not assign create no cycles`() {
        assignmentRule(a, isPresent(b, nextConditionId++))
        val action = ChangeTreeToAddConclusion(Conclusion(1, "Comment."))
        graph().cycleCreatedBy(action, isPresent(a, nextConditionId++)).shouldBeNull()
        graph().cycleCreatedBy(null, isPresent(a, nextConditionId++)).shouldBeNull()
    }

    @Test
    fun `remove and replace actions are checked against the modified attribute`() {
        // Given A depends on B
        assignmentRule(a, isPresent(b, nextConditionId++))

        // Then removing an assignment of B with a condition on A would create a cycle
        val remove = ChangeTreeToRemoveAssignment(AssignValue(b, Literal("x")))
        graph().cycleCreatedBy(remove, isPresent(a, nextConditionId++)) shouldBe listOf(b, a, b)

        // And replacing an assignment of B with an expression referencing A would create a cycle
        val replace = ChangeTreeToReplaceAssignment(
            AssignValue(b, Literal("x")),
            AssignValue(b, Formula(AttributeValue(a)))
        )
        graph().cycleCreatedBy(replace, null) shouldBe listOf(b, a, b)
    }

    @Test
    fun `a by-definition rule's dependencies come from the stored definition`() {
        // Given a rule assigning A by definition, whose stored definition references B
        val rule = ruleFactory.createRuleAndAddToParent(
            tree.root, AssignValue(a, ByDefinition), emptySet()
        )
        tree.root.addChild(rule)
        val resolver: DefinitionResolver = { attribute ->
            if (attribute == a) Formula(AttributeValue(b)) else null
        }
        val graph = DerivedAttributeDependencyGraph(tree, setOf(glucose, a, b, c), resolver)

        // Then an assignment of B conditioned on A would create a cycle
        graph.cycleCreatedBy(b, setOf(a)) shouldBe listOf(b, a, b)
    }

    @Test
    fun `a by-definition rule with no stored definition contributes no expression dependencies`() {
        // Given a rule assigning A by definition, with no stored definition
        val rule = ruleFactory.createRuleAndAddToParent(
            tree.root, AssignValue(a, ByDefinition), emptySet()
        )
        tree.root.addChild(rule)

        // Then the graph builds without error and there is no dependency of A on B
        graph().cycleCreatedBy(b, setOf(a)).shouldBeNull()
    }

    @Test
    fun `an action assigning by definition takes its references from the resolver`() {
        // Given A depends on B through a condition
        assignmentRule(a, isPresent(b, nextConditionId++))

        // When an action assigns B by definition, with a stored definition referencing A
        val action = ChangeTreeToAddAssignment(AssignValue(b, ByDefinition))
        val resolver: DefinitionResolver = { attribute ->
            if (attribute == b) Formula(AttributeValue(a)) else null
        }

        // Then the action creates a cycle, and without a definition it does not
        DerivedAttributeDependencyGraph(tree, setOf(glucose, a, b, c), resolver)
            .cycleCreatedBy(action, null) shouldBe listOf(b, a, b)
        graph().cycleCreatedBy(action, null).shouldBeNull()
    }

    @Test
    fun `cycleThrough detects a cycle introduced by a definition edit`() {
        // Given a rule assigning A by definition, and a rule assigning B conditioned on A
        val byDefinitionRule = ruleFactory.createRuleAndAddToParent(
            tree.root, AssignValue(a, ByDefinition), emptySet()
        )
        tree.root.addChild(byDefinitionRule)
        assignmentRule(b, isPresent(a, nextConditionId++))

        // When A's definition is edited to reference B, the graph built with the
        // edited resolver has a cycle through A
        val editedResolver: DefinitionResolver = { attribute ->
            if (attribute == a) Formula(AttributeValue(b)) else null
        }
        DerivedAttributeDependencyGraph(tree, setOf(glucose, a, b, c), editedResolver)
            .cycleThrough(a) shouldBe listOf(a, b, a)

        // And a harmless edit has no cycle
        val harmlessResolver: DefinitionResolver = { attribute ->
            if (attribute == a) Formula(AttributeValue(glucose)) else null
        }
        DerivedAttributeDependencyGraph(tree, setOf(glucose, a, b, c), harmlessResolver)
            .cycleThrough(a).shouldBeNull()
    }

    @Test
    fun `cycle message names the cycle`() {
        cycleMessage(listOf(a, b, a)) shouldBe "it would make \"A\" depend on itself (A → B → A)"
    }
}
