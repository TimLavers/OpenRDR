package io.rippledown.kb

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.isHigh
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Formula
import io.rippledown.model.rule.FormulaParser
import io.rippledown.model.rule.Literal
import io.rippledown.persistence.RuleStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import io.rippledown.persistence.inmemory.InMemoryConditionStore
import io.rippledown.persistence.inmemory.InMemoryRuleStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleManagerAssignmentTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var conclusionManager: ConclusionManager
    private lateinit var conditionManager: ConditionManager
    private lateinit var ruleStore: RuleStore
    private lateinit var ruleManager: RuleManager
    private lateinit var glucose: Attribute
    private lateinit var diabetesStatus: Attribute
    private lateinit var highGlucose: Condition

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        conclusionManager = ConclusionManager(InMemoryConclusionStore())
        conditionManager = ConditionManager(attributeManager, InMemoryConditionStore())
        ruleStore = InMemoryRuleStore()
        ruleManager = RuleManager(conclusionManager, conditionManager, ruleStore)

        glucose = attributeManager.getOrCreate("Glucose")
        diabetesStatus = attributeManager.getOrCreate("Diabetes status", AttributeKind.DERIVED)
        highGlucose = conditionManager.getOrCreate(isHigh(null, glucose))
    }

    @Test
    fun `an assignment rule can be created`() {
        // Given an assignment
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))

        // When a rule with the assignment is created
        val rule = ruleManager.createRuleAndAddToParent(ruleManager.ruleTree().root, assignment, setOf(highGlucose))

        // Then the rule carries the assignment and no conclusion
        ruleManager.ruleTree().size() shouldBe 2
        rule.assignment shouldBe assignment
        rule.conclusion.shouldBeNull()
        rule.conditions shouldBe setOf(highGlucose)
        rule.parent shouldBe ruleManager.ruleTree().root
    }

    @Test
    fun `an assignment rule is persisted and restored`() {
        // Given a stored rule with a literal assignment
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val rule = ruleManager.createRuleAndAddToParent(ruleManager.ruleTree().root, assignment, setOf(highGlucose))

        // When the rule tree is rebuilt from the store
        val rebuiltManager = RuleManager(conclusionManager, conditionManager, ruleStore)

        // Then the assignment is restored
        val rebuiltRule = rebuiltManager.ruleTree().ruleForId(rule.id)
        rebuiltRule.assignment shouldBe assignment
        rebuiltRule.conclusion.shouldBeNull()
        rebuiltRule.conditions shouldBe setOf(highGlucose)
    }

    @Test
    fun `a formula assignment rule is persisted and restored`() {
        // Given a stored rule with a formula assignment
        val weight = attributeManager.getOrCreate("weight")
        val height = attributeManager.getOrCreate("height")
        val bmi = attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        val parser = FormulaParser { name -> attributeManager.byName(name) }
        val expression = parser.parse("weight / (height * height)")
        val assignment = AssignValue(bmi, Formula(expression!!))
        val rule = ruleManager.createRuleAndAddToParent(ruleManager.ruleTree().root, assignment, emptySet())

        // When the rule tree is rebuilt from the store
        val rebuiltManager = RuleManager(conclusionManager, conditionManager, ruleStore)

        // Then the formula assignment is restored
        rebuiltManager.ruleTree().ruleForId(rule.id).assignment shouldBe assignment
    }

    @Test
    fun `a stopping child of an assignment rule is persisted and restored`() {
        // Given an assignment rule with a stopping child
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val assigning =
            ruleManager.createRuleAndAddToParent(ruleManager.ruleTree().root, assignment, setOf(highGlucose))
        val stopper = ruleManager.createRuleAndAddToParent(assigning, null, emptySet())

        // When the rule tree is rebuilt from the store
        val rebuiltManager = RuleManager(conclusionManager, conditionManager, ruleStore)

        // Then the structure is restored
        val rebuiltStopper = rebuiltManager.ruleTree().ruleForId(stopper.id)
        rebuiltStopper.assignment.shouldBeNull()
        rebuiltStopper.conclusion.shouldBeNull()
        rebuiltStopper.parent?.assignment shouldBe assignment
    }
}
