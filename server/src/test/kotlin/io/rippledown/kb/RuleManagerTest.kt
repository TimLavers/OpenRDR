package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.CommentFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isLow
import io.rippledown.model.condition.isNormal
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConditionStore
import io.rippledown.persistence.inmemory.InMemoryRuleStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionManager: ConditionManager
    private lateinit var ruleStore: RuleStore
    private lateinit var ruleManager: RuleManager
    private lateinit var glucose: Attribute
    private lateinit var tsh: Attribute
    private lateinit var coffeeAssignment: AssignValue
    private lateinit var teaAssignment: AssignValue
    private lateinit var champagneAssignment: AssignValue
    private lateinit var normalGlucose: Condition
    private lateinit var highTSH: Condition
    private lateinit var lowTSH: Condition
    private val text1 = "Time for coffee!"
    private val text2 = "Time for tea!"
    private val text3 = "Time for champagne!"

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        commentFactory = CommentFactory()
        conditionManager = ConditionManager(attributeManager, InMemoryConditionStore())
        ruleStore = InMemoryRuleStore()
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)

        glucose = attributeManager.getOrCreate("Glucose")
        tsh = attributeManager.getOrCreate("TSH")
        coffeeAssignment = commentFactory.comment(text1)
        teaAssignment = commentFactory.comment(text2)
        champagneAssignment = commentFactory.comment(text3)
        normalGlucose = conditionManager.getOrCreate(isNormal(null, glucose))
        highTSH = conditionManager.getOrCreate(isHigh(null, tsh))
        lowTSH = conditionManager.getOrCreate(isLow(null, tsh))
    }

    @Test
    fun `root rule is created automatically`() {
        ruleManager.ruleTree().size() shouldBe 1
        ruleManager.ruleTree().root.childRules() shouldBe emptySet()
        ruleManager.ruleTree().root.parent shouldBe null
        ruleManager.ruleTree().root.assignment shouldBe null
    }

    @Test
    fun `a stored assignment referring to an unknown attribute prevents the knowledge base from loading`() {
        // Given a persisted child rule assigning an attribute the KB does not have
        ruleStore.create(
            PersistentRule(
                id = null,
                parentId = ruleManager.ruleTree().root.id,
                conditionIds = emptySet(),
                assignment = AssignValue(Attribute(99, "Whatever"), io.rippledown.model.rule.Literal("value"))
            )
        )

        // When the rule manager rebuilds the persisted tree
        // Then the inconsistent persisted state is reported
        shouldThrow<NoSuchElementException> {
            RuleManager(conditionManager, attributeManager, ruleStore)
        }
    }

    @Test
    fun createRuleAndAddToParent() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        ruleManager.ruleTree().size() shouldBe 2
        coffeeRule.parent shouldBe root
        coffeeRule.assignment shouldBe coffeeAssignment
        coffeeRule.childRules() shouldBe emptySet()
        coffeeRule.conditions shouldBe setOf(normalGlucose, highTSH)

        // Rebuild and check.
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 2
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        rebuiltCoffeeRule.parent shouldBe ruleManager.ruleTree().root
        rebuiltCoffeeRule.assignment shouldBe coffeeAssignment
        rebuiltCoffeeRule.childRules() shouldBe emptySet()
        rebuiltCoffeeRule.conditions shouldBe setOf(normalGlucose, highTSH)
    }

    @Test
    fun deleteRule() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        ruleManager.ruleTree().size() shouldBe 2
        coffeeRule.parent shouldBe root
        ruleManager.deleteLeafRule(coffeeRule)
        ruleManager.ruleTree().size() shouldBe 1

        // Rebuild and check.
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 1
    }

    @Test
    fun cannotDeleteRuleThatIsNotALeaf() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        ruleManager.ruleTree().size() shouldBe 2
        coffeeRule.parent shouldBe root

        ruleManager.createRuleAndAddToParent(coffeeRule, teaAssignment, setOf(normalGlucose))
        ruleManager.ruleTree().size() shouldBe 3

        shouldThrow<Exception> {
            ruleManager.deleteLeafRule(coffeeRule)
        }

        // Rebuild and check.
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 3
    }

    @Test
    fun `create rule with no assignment`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        val noCoffeeRule = ruleManager.createRuleAndAddToParent(coffeeRule, null, setOf(lowTSH))
        ruleManager.ruleTree().size() shouldBe 3
        coffeeRule.childRules() shouldBe setOf(noCoffeeRule)
        noCoffeeRule.parent shouldBe coffeeRule
        noCoffeeRule.assignment shouldBe null
        noCoffeeRule.childRules() shouldBe emptySet()
        noCoffeeRule.conditions shouldBe setOf(lowTSH)

        // Rebuild and check.
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 3
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        val rebuiltNoCoffeeRule = rebuiltCoffeeRule.childRules().single()
        rebuiltCoffeeRule.childRules() shouldBe setOf(noCoffeeRule)
        rebuiltNoCoffeeRule.parent shouldBe coffeeRule
        rebuiltNoCoffeeRule.assignment shouldBe null
        rebuiltNoCoffeeRule.childRules() shouldBe emptySet()
        rebuiltNoCoffeeRule.conditions shouldBe setOf(lowTSH)
    }

    @Test
    fun `create rule with no conditions`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        val champagneRule = ruleManager.createRuleAndAddToParent(coffeeRule, champagneAssignment, setOf())
        ruleManager.ruleTree().size() shouldBe 3
        coffeeRule.childRules() shouldBe setOf(champagneRule)
        champagneRule.parent shouldBe coffeeRule
        champagneRule.assignment shouldBe champagneAssignment
        champagneRule.childRules() shouldBe emptySet()
        champagneRule.conditions shouldBe emptySet()

        // Rebuild and check.
        ruleManager = RuleManager(conditionManager, attributeManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 3
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        val rebuiltChampagneRule = rebuiltCoffeeRule.childRules().single()
        rebuiltCoffeeRule.childRules() shouldBe setOf(champagneRule)
        rebuiltChampagneRule.parent shouldBe coffeeRule
        rebuiltChampagneRule.assignment shouldBe champagneAssignment
        rebuiltChampagneRule.childRules() shouldBe emptySet()
        rebuiltChampagneRule.conditions shouldBe emptySet()
    }

    @Test
    fun `cannot create a rule that has parent not in tree`() {
        shouldThrow<IllegalArgumentException> {
            ruleManager.createRuleAndAddToParent(Rule(100, null, mutableSetOf()), teaAssignment, setOf(lowTSH))
        }.message shouldBe "Parent rule not in tree."
    }

    @Test
    fun `cannot restore if more than one rule has no parent`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeAssignment, setOf(normalGlucose, highTSH))
        ruleManager.createRuleAndAddToParent(coffeeRule, champagneAssignment, setOf())
        ruleStore.create(PersistentRule(null, null, emptySet(), teaAssignment))

        shouldThrow<IllegalArgumentException> {
            RuleManager(conditionManager, attributeManager, ruleStore)
        }.message shouldBe "Rule tree could not be rebuilt as more than one rule lacks a parent."
    }
}