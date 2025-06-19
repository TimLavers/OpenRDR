package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.rippledown.model.Conclusion
import kotlin.test.Test

internal class RuleTest : RuleTestBase() {
    private val conclusion1 = Conclusion(1, "First conclusion")
    private val conclusion2 = Conclusion(2, "Second conclusion")
    private val conclusion3 = Conclusion(3, "Third conclusion")

    @Test
    fun `action summary for root rule`() {
        val rule = Rule(100, null, null)
        rule.actionSummary() shouldBe ""
    }

    @Test
    fun `action summary for rule adding a conclusion`() {
        val rule = Rule(100, null, null, setOf(createCondition("a")))
        val childRule = Rule(200, null, conclusion2, setOf(createCondition("b")))
        rule.addChild(childRule)
        childRule.actionSummary() shouldBe "Rule to add comment:\n${conclusion2.truncatedText()}"
    }

    @Test
    fun `action summary for rule removing a conclusion`() {
        val rule = Rule(100, null, conclusion1, setOf(createCondition("a")))
        val childRule = Rule(200, null, null, setOf(createCondition("b")))
        rule.addChild(childRule)
        childRule.actionSummary() shouldBe "Rule to remove comment:\n${conclusion1.truncatedText()}"
    }

    @Test
    fun `action summary for rule replacing a conclusion`() {
        val rule = Rule(100, null, conclusion1, setOf(createCondition("a")))
        val childRule = Rule(200, null, conclusion2, setOf(createCondition("b")))
        rule.addChild(childRule)
        val expected = """
            Rule to replace comment:
            ${conclusion1.truncatedText()}
            with:
            ${conclusion2.truncatedText()}
        """.trimIndent()
        childRule.actionSummary() shouldBe expected
    }

    @Test
    fun `action summary truncates text`() {
        val longText = "This is a long conclusion."
        val longConclusion = Conclusion(1, longText)
        val root = Rule(90, null, null)
        val rule = Rule(100, null, longConclusion, setOf(createCondition("a")))
        root.addChild(rule)
        rule.actionSummary() shouldEndWith longConclusion.truncatedText()
        val longerText = "The previous conclusion was not long enough."
        val longerConclusion = Conclusion(2, longerText)

        val replacer = Rule(110, null, longerConclusion )
        rule.addChild(replacer)
        replacer.actionSummary() shouldContain longConclusion.truncatedText()
        replacer.actionSummary() shouldContain longerConclusion.truncatedText()

        val remover = Rule(120, null, null)
        replacer.addChild(remover)
        replacer.actionSummary() shouldEndWith longerConclusion.truncatedText()
    }

    @Test
    fun `adding a child in the constructor should set the parent`() {
        val child = Rule(10, null, conclusion2, setOf())
        val rule = Rule(1, null, conclusion1, setOf(), mutableSetOf(child))
        child.parent shouldBe rule
    }

    @Test
    fun `adding a child should set the parent`() {
        val child = Rule(10, null, conclusion2, setOf())
        val rule = Rule(1, null, conclusion1, setOf())
        rule.addChild(child)
        child.parent shouldBe rule
    }

    @Test
    fun `remove a child leaf rule`() {
        val root = Rule(10, null, null)
        val rule12 = Rule(12, root, conclusion1, setOf())
        root.addChild(rule12)
        val rule13 = Rule(13, root, conclusion2, setOf())
        rule12.addChild(rule13)
        val rule14 = Rule(14, root, conclusion2, setOf())
        rule12.addChild(rule14)

        shouldThrow<Exception> {
            root.removeChildLeafRule(rule12)
        }.message shouldBe "Only a leaf rule can be removed."

        shouldThrow<Exception> {
            root.removeChildLeafRule(rule13)
        }.message shouldBe "Leaf rule is not a child of this rule."

        rule12.childRules() shouldBe listOf(rule13, rule14)
        rule12.removeChildLeafRule(rule14)
        rule12.childRules() shouldBe listOf(rule13)
        rule14.parent shouldBe null
    }

    @Test
    fun `should be structurally equal if same conditions conclusion and parent even if different children`() {
        val child1 = Rule(11, null, conclusion2, setOf())
        val child2 = Rule(12, null, conclusion2, setOf())
        val rule1 = Rule(1, null, conclusion1, setOf(), mutableSetOf(child1))
        val rule2 = Rule(2, null, conclusion1, setOf(), mutableSetOf(child2))
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe true
    }

    @Test
    fun `should be structurally equal if identical`() {
        val rule1 = Rule(1, null, conclusion1, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun `should be structurally equal if identical and null conclusion`() {
        val rule1 = Rule(2, null, null, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun `should not be structurally equal if different conditions`() {
        val rule1 = Rule(1, null, conclusion1, setOf(createCondition("a")))
        val rule2 = Rule(1, null, conclusion1, setOf())
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `should not be structurally equal to a root rule`() {
        val root = Rule(0, null, null, setOf())
        val rule = Rule(1, root, conclusion1, setOf(createCondition("a")))
        root shouldNotBe rule
        rule.structurallyEqual(root) shouldBe false
        rule shouldNotBe root
        root.structurallyEqual(rule) shouldBe false
    }

    @Test
    fun `should not be structurally equal if different conclusion`() {
        val rule1 = Rule(1, null, conclusion1)
        val rule2 = Rule(2, null, conclusion2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `should not be structurally equal if different parents`() {
        val parent1 = Rule(1, null, conclusion1)
        val parent2 = Rule(2, null, conclusion2)
        val rule1 = Rule(11, null, conclusion1)
        val rule2 = Rule(12, null, conclusion1)
        parent1.addChild(rule1)
        parent2.addChild(rule2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun `conditions are satisfied if empty`() {
        val rule = Rule(1, null, conclusion1)
        rule.conditionsSatisfied(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun `single condition which is true for case`() {
        val rule = Rule(1, null, conclusion1, setOf(createCondition("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardvark")) shouldBe true
    }

    @Test
    fun `single condition which is false for case`() {
        val rule = Rule(1, null, conclusion1, setOf(createCondition("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardwolf")) shouldBe false
    }

    @Test
    fun `any condition false means rule does not apply`() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(1, null, conclusion1, conditions)
        rule.conditionsSatisfied(clinicalNotesCase("abc")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("abd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cbd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cba")) shouldBe false
    }

    @Test
    fun `rule applies if all true`() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(1, null, conclusion1, conditions)
        rule.conditionsSatisfied(clinicalNotesCase("abcd")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("bcda")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("xdcba")) shouldBe true
    }

    @Test
    fun summary() {
        val condA = createCondition("a")
        val condB = createCondition("b")
        val conditions = setOf(condA, condB)
        val rule1 = Rule(1, null, null, conditions)
        rule1.summary().conclusion shouldBe null
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContain condA
        rule1.summary().conditions shouldContain condB

        val rule2 = Rule(2, null, conclusion1, conditions)
        rule2.summary().conclusion shouldBe conclusion1
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContain condA
        rule1.summary().conditions shouldContain condB
    }

    @Test
    fun `summary should contain conditions from root`() {
        val conditions1 = setOf(createCondition("a"), createCondition("b"))
        val rule1 = Rule(1, null, null, conditions1)
        rule1.summary().conditionTextsFromRoot shouldBe listOf(
            createCondition("a"),
            createCondition("b")
        ).map { it.asText() }

        val conditions2 = setOf(createCondition("x"), createCondition("y"))
        val rule2 = Rule(2, rule1, conclusion2, conditions2)
        rule2.summary().conditionTextsFromRoot shouldBe listOf(
            createCondition("a"),
            createCondition("b"),
            createCondition("x"),
            createCondition("y")
        ).map {
            it.asText()
        }
    }

    @Test
    fun `rule with no children that applies to case`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)
        val kase = clinicalNotesCase("ab")

        val result = rule.apply(kase, interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun `rule that does not apply to case and has no children`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule applies to case but child does not`() {
        val rule = setupRuleWithOneChild()
        val result = rule.apply(clinicalNotesCase("ac"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun `rule applies to case and so does child`() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(3, null, conclusion2, childConditions)
        rule.addChild(childRule)

        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion2)
    }

    @Test
    fun `rule does not apply to case but child does`() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule does not apply to case nor does child`() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("xy"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun `rule applies no child does`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("a"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun `rule applies and one child does`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion2)
    }

    @Test
    fun `rule applies and so do both children`() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion2, conclusion3)
    }

    @Test
    fun addRuleTest() {
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(12, null, conclusion3, grandChildConditions)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(13, null, conclusion2, childConditions)
        childRule.addChild(grandChild)
        childRule.conditions shouldBe childRule.conditions
        childRule.conclusion shouldBe childRule.conclusion
        val rootConditions = setOf(createCondition("a"), createCondition("b"))
        val root = Rule(45, null, conclusion1, rootConditions)
        root.addChild(childRule)
        root.conclusion shouldBe root.conclusion
        root.conditions shouldBe root.conditions
        root.childRules() shouldContain (childRule)
        val result = root.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion3)
    }

    @Test
    fun visitTest() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)
        val visited = mutableSetOf<Rule>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it)
        }
        rule.visit(action)
        visited.size shouldBe 1
        visited shouldContain (rule)
    }

    @Test
    fun `visit rule with children`() {
        val rule = setupRuleWithTwoChildren()
        val visited = mutableSetOf<Conclusion?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.conclusion)
        }
        rule.visit(action)
        val expected = mutableSetOf(conclusion1, conclusion2, conclusion3)
        visited shouldBe expected
    }

    @Test
    fun `visit deep`() {
        val rule = setupRuleWithOneChild()
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(1, null, conclusion3, grandChildConditions)
        rule.childRules().first().addChild(grandChild)

        val visited = mutableSetOf<Conclusion?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.conclusion)
        }
        rule.visit(action)
        val expected = mutableSetOf(conclusion1, conclusion2, conclusion3)
        visited shouldBe expected
    }

    @Test
    fun `rule should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.conclusion shouldBe rule.conclusion
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `rule with null parent should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe rule.parent
        copy.conclusion shouldBe rule.conclusion
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `rule with not null parent should be copied`() {
        val rule = setupRuleWithOneChild()
        rule.parent = Rule(1, null)
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe Rule(1, null)
        copy.conclusion shouldBe rule.conclusion
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun `child rules should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyChild = copy.childRules().iterator().next()
        val ruleChild = rule.childRules().iterator().next()
        copyChild shouldBe ruleChild
        (copyChild !== ruleChild) shouldBe true
    }

    @Test
    fun `conditions should be copied`() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyCondition = copy.conditions.iterator().next()
        val ruleCondition = rule.conditions.iterator().next()
        copyCondition shouldBe ruleCondition
    }

    @Test
    fun `should list conditions for rule with null parent`() {
        val child = Rule(99, null, conclusion1, setOf(createCondition("a"), createCondition("b")))
        child.conditionTextsFromRoot() shouldBe listOf(createCondition("a"), createCondition("b")).map { it.asText() }
    }

    @Test
    fun `should list conditions for rule with not null parent`() {
        val parent =
            Rule(23, null, conclusion1, setOf(createCondition("x"), createCondition("y"), createCondition("z")))
        val child =
            Rule(24, parent, conclusion2, setOf(createCondition("a"), createCondition("b"), createCondition("c")))
        child.conditionTextsFromRoot() shouldBe listOf(
            createCondition("x"),
            createCondition("y"),
            createCondition("z"),
            createCondition("a"),
            createCondition("b"),
            createCondition("c")
        ).map { it.asText() }

    }

    private fun setupRuleWithTwoChildren(): Rule {
        val rule = setupRuleWithOneChild()
        val childConditions = setOf(createCondition("c"))
        val childRule = Rule(12, null, conclusion3, childConditions)
        rule.addChild(childRule)
        return rule
    }

    private fun setupRuleWithOneChild(): Rule {
        val rule = Rule(100, null, conclusion1, setOf(createCondition("a")))
        val childRule = Rule(200, null, conclusion2, setOf(createCondition("b")))
        rule.addChild(childRule)
        return rule
    }

    private fun checkInterpretation(vararg conclusions: Conclusion) {
        checkInterpretation(interpretation, *conclusions)
    }
}