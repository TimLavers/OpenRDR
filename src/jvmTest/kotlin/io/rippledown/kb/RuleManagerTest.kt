package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.condition.IsLow
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.rule.Rule
import io.rippledown.persistence.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class RuleManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var conclusionManager: ConclusionManager
    private lateinit var conditionManager: ConditionManager
    private lateinit var ruleStore: RuleStore
    private lateinit var ruleManager: RuleManager
    private lateinit var glucose: Attribute
    private lateinit var tsh: Attribute
    private lateinit var coffeeConclusion: Conclusion
    private lateinit var teaConclusion: Conclusion
    private lateinit var champagneConclusion: Conclusion
    private lateinit var normalGlucose: Condition
    private lateinit var highTSH: Condition
    private lateinit var lowTSH: Condition
    private val text1 = "Time for coffee!"
    private val text2 = "Time for tea!"
    private val text3 = "Time for champagne!"

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        conclusionManager = ConclusionManager(InMemoryConclusionStore())
        conditionManager = ConditionManager(attributeManager, InMemoryConditionStore())
        ruleStore = InMemoryRuleStore()
        ruleManager = RuleManager(conclusionManager, conditionManager, ruleStore)

        glucose = attributeManager.getOrCreate("Glucose")
        tsh = attributeManager.getOrCreate("TSH")
        coffeeConclusion = conclusionManager.getOrCreate(text1)
        teaConclusion = conclusionManager.getOrCreate(text2)
        champagneConclusion = conclusionManager.getOrCreate(text3)
        normalGlucose = conditionManager.getOrCreate(IsNormal(null, glucose))
        highTSH = conditionManager.getOrCreate(IsHigh(null, tsh))
        lowTSH = conditionManager.getOrCreate(IsLow(null, tsh))
    }

    @Test
    fun `root rule is created automatically`() {
        ruleManager.ruleTree().size() shouldBe 1
        ruleManager.ruleTree().root.childRules() shouldBe emptySet()
        ruleManager.ruleTree().root.parent shouldBe null
        ruleManager.ruleTree().root.conclusion shouldBe null
    }

    @Test
    fun createRuleAndAddToParent() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeConclusion, setOf(normalGlucose, highTSH))
        ruleManager.ruleTree().size() shouldBe 2
        coffeeRule.parent shouldBe root
        coffeeRule.conclusion shouldBe coffeeConclusion
        coffeeRule.childRules() shouldBe emptySet()
        coffeeRule.conditions shouldBe setOf(normalGlucose, highTSH)

        // Rebuild and check.
        ruleManager = RuleManager(conclusionManager, conditionManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 2
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        rebuiltCoffeeRule.parent shouldBe ruleManager.ruleTree().root
        rebuiltCoffeeRule.conclusion shouldBe coffeeConclusion
        rebuiltCoffeeRule.childRules() shouldBe emptySet()
        rebuiltCoffeeRule.conditions shouldBe setOf(normalGlucose, highTSH)
    }

    @Test
    fun `create rule with no conclusion`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeConclusion, setOf(normalGlucose, highTSH))
        val noCoffeeRule = ruleManager.createRuleAndAddToParent(coffeeRule, null, setOf(lowTSH))
        ruleManager.ruleTree().size() shouldBe 3
        coffeeRule.childRules() shouldBe setOf(noCoffeeRule)
        noCoffeeRule.parent shouldBe coffeeRule
        noCoffeeRule.conclusion shouldBe null
        noCoffeeRule.childRules() shouldBe emptySet()
        noCoffeeRule.conditions shouldBe setOf(lowTSH)

        // Rebuild and check.
        ruleManager = RuleManager(conclusionManager, conditionManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 3
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        val rebuiltNoCoffeeRule = rebuiltCoffeeRule.childRules().single()
        rebuiltCoffeeRule.childRules() shouldBe setOf(noCoffeeRule)
        rebuiltNoCoffeeRule.parent shouldBe coffeeRule
        rebuiltNoCoffeeRule.conclusion shouldBe null
        rebuiltNoCoffeeRule.childRules() shouldBe emptySet()
        rebuiltNoCoffeeRule.conditions shouldBe setOf(lowTSH)
    }

    @Test
    fun `create rule with no conditions`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeConclusion, setOf(normalGlucose, highTSH))
        val champagneRule = ruleManager.createRuleAndAddToParent(coffeeRule, champagneConclusion, setOf())
        ruleManager.ruleTree().size() shouldBe 3
        coffeeRule.childRules() shouldBe setOf(champagneRule)
        champagneRule.parent shouldBe coffeeRule
        champagneRule.conclusion shouldBe champagneConclusion
        champagneRule.childRules() shouldBe emptySet()
        champagneRule.conditions shouldBe emptySet()

        // Rebuild and check.
        ruleManager = RuleManager(conclusionManager, conditionManager, ruleStore)
        ruleManager.ruleTree().size() shouldBe 3
        val rebuiltCoffeeRule = ruleManager.ruleTree().root.childRules().single()
        val rebuiltChampagneRule = rebuiltCoffeeRule.childRules().single()
        rebuiltCoffeeRule.childRules() shouldBe setOf(champagneRule)
        rebuiltChampagneRule.parent shouldBe coffeeRule
        rebuiltChampagneRule.conclusion shouldBe champagneConclusion
        rebuiltChampagneRule.childRules() shouldBe emptySet()
        rebuiltChampagneRule.conditions shouldBe emptySet()
    }

    @Test
    fun `cannot create a rule that has parent not in tree`() {
        shouldThrow<IllegalArgumentException> {
            ruleManager.createRuleAndAddToParent(Rule(100, null, null, mutableSetOf()), teaConclusion, setOf(lowTSH))
        }.message shouldBe "Parent rule not in tree."
    }

    @Test
    fun `cannot restore if more than one rule has no parent`() {
        val root = ruleManager.ruleTree().root

        val coffeeRule = ruleManager.createRuleAndAddToParent(root, coffeeConclusion, setOf(normalGlucose, highTSH))
        ruleManager.createRuleAndAddToParent(coffeeRule, champagneConclusion, setOf())
        ruleStore.create(PersistentRule(null, null, teaConclusion.id, emptySet()))

        shouldThrow<IllegalArgumentException> {
            RuleManager(conclusionManager, conditionManager, ruleStore)
        }.message shouldBe "Rule tree could not be rebuilt as more than one rule lacks a parent."
    }
}