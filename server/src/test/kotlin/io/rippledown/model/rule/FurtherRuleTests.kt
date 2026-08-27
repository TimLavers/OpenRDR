package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.util.shouldContainSameAs
import kotlin.test.Test

internal class FurtherRuleTests : RuleTestBase() {
    private val comment1 = comment("First comment")
    private val comment2 = comment("Second comment")
    private val comment3 = comment("Third comment")

    @Test
    fun adding_a_child_in_the_constructor_should_set_the_parent() {
        val child = Rule(3, null, setOf(), mutableSetOf(), comment2)
        val rule = Rule(5, null, setOf(), mutableSetOf(child), comment1)
        child.parent shouldBe rule
    }

    @Test
    fun adding_a_child_should_set_the_parent() {
        val child = Rule(4, null, setOf(), mutableSetOf(), comment2)
        val rule = Rule(5, null, setOf(), mutableSetOf(), comment1)
        rule.addChild(child)
        child.parent shouldBe rule
    }

    @Test
    fun should_be_structurally_equal_if_same_conditions_assignment_and_parent_even_if_different_children() {
        val child1 = Rule(11, null, setOf(), mutableSetOf(), comment2)
        val child2 = Rule(12, null, setOf(), mutableSetOf(), comment2)
        val rule1 = Rule(1, null, setOf(), mutableSetOf(child1), comment1)
        val rule2 = Rule(2, null, setOf(), mutableSetOf(child2), comment1)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe true
    }

    @Test
    fun should_be_structurally_equal_if_identical() {
        val rule1 = Rule(0, null, setOf(), mutableSetOf(), comment1)
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun should_be_structurally_equal_if_identical_and_null_assignment() {
        val rule1 = Rule(0, null, setOf())
        rule1 shouldBe rule1
        rule1.structurallyEqual(rule1) shouldBe true
    }

    @Test
    fun should_not_be_structurally_equal_if_different_conditions() {
        val rule1 = Rule(0, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val rule2 = Rule(0, null, setOf(), mutableSetOf(), comment1)
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_to_a_root_rule() {
        val root = Rule(0, null, setOf())
        val rule = Rule(1, root, setOf(createCondition("a")), mutableSetOf(), comment1)
        root shouldNotBe rule
        rule.structurallyEqual(root) shouldBe false
        rule shouldNotBe root
        root.structurallyEqual(rule) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_if_different_assignment() {
        val rule1 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        val rule2 = Rule(2, null, setOf(), mutableSetOf(), comment2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun should_not_be_structurally_equal_if_different_parents() {
        val parent1 = Rule(1, null, setOf(), mutableSetOf(), comment1)
        val parent2 = Rule(2, null, setOf(), mutableSetOf(), comment2)
        val rule1 = Rule(11, null, setOf(), mutableSetOf(), comment1)
        val rule2 = Rule(12, null, setOf(), mutableSetOf(), comment1)
        parent1.addChild(rule1)
        parent2.addChild(rule2)
        rule1 shouldNotBe rule2
        rule1.structurallyEqual(rule2) shouldBe false
        rule2.structurallyEqual(rule1) shouldBe false
    }

    @Test
    fun conditions_are_satisfied_if_empty() {
        val rule = Rule(0, null, setOf(), mutableSetOf(), comment1)
        rule.conditionsSatisfied(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun single_condition_which_is_true_for_case() {
        val rule = Rule(0, null, setOf(createCondition("vark")), mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("aardvark")) shouldBe true
    }

    @Test
    fun single_condition_which_is_false_for_case() {
        val rule = Rule(0, null, setOf(createCondition("vark")), mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("aardwolf")) shouldBe false
    }

    @Test
    fun any_condition_false_means_rule_does_not_apply() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("abc")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("abd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cbd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cba")) shouldBe false
    }

    @Test
    fun rule_applies_if_all_true() {
        val conditions = setOf(createCondition("a"), createCondition("b"), createCondition("c"), createCondition("d"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)
        rule.conditionsSatisfied(clinicalNotesCase("abcd")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("bcda")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("xdcba")) shouldBe true
    }

    @Test
    fun summary() {
        val conditions = setOf(createCondition("a"), createCondition("b"))
        val rule1 = Rule(1, null, conditions)
        rule1.summary().assignment shouldBe null
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContainSameAs createCondition("a")
        rule1.summary().conditions shouldContainSameAs  createCondition("b")

        val rule2 = Rule(2, null, conditions, mutableSetOf(), comment1)
        rule2.summary().assignment shouldBe comment1
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContainSameAs  createCondition("a")
        rule1.summary().conditions shouldContainSameAs  createCondition("b")
    }

    @Test
    fun rule_with_no_children_that_applies_to_case() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)
        val kase = clinicalNotesCase("ab")

        val result = rule.apply(kase, interpretation)
        result shouldBe true
        checkInterpretation(comment1)
    }

    @Test
    fun rule_that_does_not_apply_to_case_and_has_no_children() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)

        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        checkInterpretation()//empty
    }

    @Test
    fun rule_applies_to_case_but_child_does_not() {
        val rule = setupRuleWithOneChild()
        val result = rule.apply(clinicalNotesCase("ac"), interpretation)
        result shouldBe true
        checkInterpretation(comment1)
    }

    @Test
    fun rule_applies_to_case_and_so_does_child() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(2, null, childConditions, mutableSetOf(), comment2)
        rule.addChild(childRule)

        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(comment2)
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
        checkInterpretation(comment1)
    }

    @Test
    fun rule_applies_and_one_child_does() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        checkInterpretation(comment2)
    }

    @Test
    fun rule_applies_and_so_do_both_children() {
        val rule = setupRuleWithTwoChildren()
        val result = rule.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(comment2, comment3)
    }

    @Test
    fun addRuleTest() {
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(100, null, grandChildConditions, mutableSetOf(), comment3)
        val childConditions = setOf(createCondition("b"))
        val childRule = Rule(10, null, childConditions, mutableSetOf(), comment2)
        childRule.addChild(grandChild)
        childRule.conditions shouldBe  childRule.conditions
        childRule.assignment shouldBe childRule.assignment
        val rootConditions = setOf(createCondition("a"), createCondition("b"))
        val root = Rule(0, null, rootConditions, mutableSetOf(), comment1)
        root.addChild(childRule)
        root.assignment shouldBe root.assignment
        root.conditions shouldBe root.conditions
        root.childRules() shouldContain(childRule)
        val result = root.apply(clinicalNotesCase("abc"), interpretation)
        result shouldBe true
        checkInterpretation(comment3)
    }

    @Test
    fun visitTest() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(0, null, conditions, mutableSetOf(), comment1)
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
        val visited = mutableSetOf<AssignValue?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.assignment)
        }
        rule.visit(action)
        val expected = mutableSetOf(comment1, comment2, comment3)
        visited shouldBe expected
    }

    @Test
    fun visit_deep() {
        val rule = setupRuleWithOneChild()
        val grandChildConditions = setOf(createCondition("a"), createCondition("c"))
        val grandChild = Rule(0, null, grandChildConditions, mutableSetOf(), comment3)
        rule.childRules().first().addChild(grandChild)

        val visited = mutableSetOf<AssignValue?>()
        val action: ((Rule) -> (Unit)) = {
            visited.add(it.assignment)
        }
        rule.visit(action)
        val expected = mutableSetOf(comment1, comment2, comment3)
        visited shouldBe expected
    }

    @Test
    fun rule_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.assignment shouldBe rule.assignment
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun rule_with_null_parent_should_be_copied() {
        val rule = setupRuleWithOneChild()
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe rule.parent
        copy.assignment shouldBe rule.assignment
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
    }

    @Test
    fun rule_with_not_null_parent_should_be_copied() {
        val rule = setupRuleWithOneChild()
        rule.parent = Rule(0, null)
        val copy = rule.copy()
        (copy !== rule) shouldBe true
        copy.parent shouldBe Rule(0, null)
        copy.assignment shouldBe rule.assignment
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

    private fun setupRuleWithTwoChildren(): Rule {
        val rule = setupRuleWithOneChild()
        val childConditions = setOf(createCondition("c"))
        val childRule = Rule(2, null, childConditions, mutableSetOf(), comment3)
        rule.addChild(childRule)
        return rule
    }

    private fun setupRuleWithOneChild(): Rule {
        val rule = Rule(3, null, setOf(createCondition("a")), mutableSetOf(), comment1)
        val childRule = Rule(13, null, setOf(createCondition("b")), mutableSetOf(), comment2)
        rule.addChild(childRule)
        return rule
    }

    private fun checkInterpretation(vararg assignments: AssignValue) {
        checkInterpretation(interpretation, *assignments)
    }
}