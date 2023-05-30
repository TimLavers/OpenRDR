package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Conclusion
import kotlin.test.Test

internal class RuleTest : RuleTestBase() {
    private val conclusion1 = Conclusion(1, "First conclusion")
    private val conclusion2 = Conclusion(2, "Second conclusion")
    private val conclusion3 = Conclusion(3, "Third conclusion")

    @Test
    fun adding_a_child_in_the_constructor_should_set_the_parent() {
        val child = Rule(10, null, conclusion2, setOf())
        val rule = Rule(1, null, conclusion1, setOf(), mutableSetOf(child))
        child.parent shouldBe rule
    }

    @Test
    fun adding_a_child_should_set_the_parent() {
        val child = Rule(10, null, conclusion2, setOf())
        val rule = Rule(1, null, conclusion1, setOf())
        rule.addChild(child)
        child.parent shouldBe rule
    }

    @Test
    fun should_be_structurally_equal_if_same_conditions_conclusion_and_parent_even_if_different_children() {
        val child1 = Rule(11, null, conclusion2, setOf())
        val child2 = Rule(12, null, conclusion2, setOf())
        val rule1 = Rule(1, null, conclusion1, setOf(), mutableSetOf(child1))
        val rule2 = Rule(2, null, conclusion1, setOf(), mutableSetOf(child2))
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe true
    }

    @Test
    fun should_be_structurally_equal_if_identical() {
        val rule1 = Rule(1, null, conclusion1, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun should_be_structurally_equal_if_identical_and_null_conclusion() {
        val rule1 = Rule(2, null, null, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun should_not_be_structurally_equal_if_different_conditions() {
        val rule1 = Rule(1, null, conclusion1, setOf(createCondition("a")))
        val rule2 = Rule(1, null, conclusion1, setOf())
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_to_a_root_rule() {
        val root = Rule(0, null, null, setOf())
        val rule = Rule(1, root, conclusion1, setOf(createCondition("a")))
        root shouldNotBe rule
        rule.structurallyEqual(root) shouldBe false
        rule shouldNotBe root
        root.structurallyEqual(rule) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_if_different_conclusion() {
        val rule1 = Rule(1, null, conclusion1)
        val rule2 = Rule(2, null, conclusion2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_if_different_parents() {
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
    fun conditions_are_satisfied_if_empty() {
        val rule = Rule(1, null, conclusion1)
        rule.conditionsSatisfied(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun single_condition_which_is_true_for_case() {
        val rule = Rule(1, null, conclusion1, setOf(createCondition("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardvark")) shouldBe true
    }

    @Test
    fun single_condition_which_is_false_for_case() {
        val rule = Rule(1, null, conclusion1, setOf(createCondition("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardwolf")) shouldBe false
    }

    @Test
    fun any_condition_false_means_rule_does_not_apply() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(1, null, conclusion1, conditions)
        rule.conditionsSatisfied(clinicalNotesCase("abc")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("abd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cbd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cba")) shouldBe false
    }

    @Test
    fun rule_applies_if_all_true() {
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
    fun summary_should_contain_conditions_from_root() {
        val conditions1 = setOf(cond("a"), cond("b"))
        val rule1 = Rule("r1", null, null, conditions1)
        rule1.summary().conditionTextsFromRoot shouldBe listOf(cond("a"), cond("b")).map { it.asText() }

        val conditions2 = setOf(cond("x"), cond("y"))
        val rule2 = Rule("r2", rule1, conclusion2, conditions2)
        rule2.summary().conditionTextsFromRoot shouldBe listOf(
            cond("a"),
            cond("b"),
            cond("x"),
            cond("y")
        ).map {
            it.asText()
        }
    }

    @Test
    fun rule_with_no_children_that_applies_to_case() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)
        val kase = clinicalNotesCase("ab")

        val result = rule.apply(kase, interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun rule_that_does_not_apply_to_case_and_has_no_children() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(1, null, conclusion1, conditions)

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun rule_applies_to_case_but_child_does_not() {
        val rule = setupRuleWithOneChild()
        val result = rule.apply(clinicalNotesCase("ac"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun rule_applies_to_case_and_so_does_child() {
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
    fun rule_does_not_apply_to_case_but_child_does() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun rule_does_not_apply_to_case_nor_does_child() {
        val rule = setupRuleWithOneChild()

        val result = rule.apply(clinicalNotesCase("xy"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun rule_applies_no_child_does() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("a"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    @Test
    fun rule_applies_and_one_child_does() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(conclusion2)
    }

    @Test
    fun rule_applies_and_so_do_both_children() {
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
    fun visit_rule_with_children() {
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
    fun visit_deep() {
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
    fun rule_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.conclusion shouldBe rule.conclusion
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun rule_with_null_parent_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe rule.parent
        copy.conclusion shouldBe rule.conclusion
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun rule_with_not_null_parent_should_be_copied() {
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
    fun child_rules_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyChild = copy.childRules().iterator().next()
        val ruleChild = rule.childRules().iterator().next()
        copyChild shouldBe ruleChild
        (copyChild !== ruleChild) shouldBe true
    }

    @Test
    fun conditions_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        val copyCondition = copy.conditions.iterator().next()
        val ruleCondition = rule.conditions.iterator().next()
        copyCondition shouldBe ruleCondition
    }

    @Test
    fun should_list_conditions_for_rule_with_null_parent() {
        val child = Rule("c", null, conclusion1, setOf(cond("a"), cond("b")))
        child.conditionTextsFromRoot() shouldBe listOf(cond("a"), cond("b")).map { it.asText() }
    }

    @Test
    fun should_list_conditions_for_rule_with_not_null_parent() {
        val parent = Rule("r", null, conclusion1, setOf(cond("x"), cond("y"), cond("z")))
        val child = Rule("c", parent, conclusion2, setOf(cond("a"), cond("b"), cond("c")))
        child.conditionTextsFromRoot() shouldBe listOf(
            cond("x"),
            cond("y"),
            cond("z"),
            cond("a"),
            cond("b"),
            cond("c")
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