package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.rule.Rule
import kotlin.test.Test

internal class RuleTest : ConditionTestBase() {
    private val conclusion1 = conc("First conclusion")
    private val conclusion2 = conc("Second conclusion")
    private val conclusion3 = conc("Third conclusion")
    private val caseId = CaseId("Case1", "Case1")
    private val interpretation = Interpretation(caseId, "")

    private fun conc(text: String): Conclusion {
        return Conclusion(text)
    }

    private fun cond(text: String): Condition {
        return ContainsText(clinicalNotes, text)
    }

    @Test
    fun adding_a_child_in_the_constructor_should_set_the_parent() {
        val child = Rule(null, conclusion2, setOf())
        val rule = Rule(null, conclusion1, setOf(), mutableSetOf(child))
        child.parent shouldBe rule
    }

    @Test
    fun adding_a_child_should_set_the_parent() {
        val child = Rule(null, conclusion2, setOf())
        val rule = Rule(null, conclusion1, setOf())
        rule.addChild(child)
        child.parent shouldBe rule
    }

    @Test
    fun should_be_equal_if_same_conditions_conclusion_and_parent_even_if_different_children() {
        val child1 = Rule(null, conclusion2, setOf())
        val child2 = Rule(null, conclusion2, setOf())
        val rule1 = Rule(null, conclusion1, setOf(), mutableSetOf(child1))
        val rule2 = Rule(null, conclusion1, setOf(), mutableSetOf(child2))
        rule1 shouldBe rule2
    }

    @Test
    fun should_be_equal_if_identical() {
        val rule1 = Rule(null, conclusion1, setOf())
        rule1 shouldBe rule1
    }

    @Test
    fun should_be_equal_if_identical_and_null_conclusion() {
        val rule1 = Rule(null, null, setOf())
        rule1 shouldBe rule1
    }

    @Test
    fun should_not_be_equal_if_different_conditions() {
        val rule1 = Rule(null, conclusion1, setOf(cond("a")))
        val rule2 = Rule(null, conclusion1, setOf())
        rule1 shouldNotBe rule2
    }

    @Test
    fun should_not_be_equal_to_a_root_rule() {
        val root = Rule(null, null, setOf())
        val rule = Rule(root, conclusion1, setOf(cond("a")))
        root shouldNotBe rule
        rule shouldNotBe root
    }

    @Test
    fun should_not_be_equal_if_different_conclusion() {
        val rule1 = Rule(null, conclusion1)
        val rule2 = Rule(null, conclusion2)
        rule1 shouldNotBe rule2
    }

    @Test
    fun should_not_be_equal_if_different_parents() {
        val parent1 = Rule(null, conclusion1)
        val parent2 = Rule(null, conclusion2)
        val rule1 = Rule(null, conclusion1)
        val rule2 = Rule(null, conclusion1)
        parent1.addChild(rule1)
        parent2.addChild(rule2)
        rule1 shouldNotBe rule2
    }

    @Test
    fun conditions_are_satisfied_if_empty() {
        val rule = Rule(null, conclusion1)
        rule.conditionsSatisfied(glucoseOnlyCase()) shouldBe true
    }

    @Test
    fun single_condition_which_is_true_for_case() {
        val rule = Rule(null, conclusion1, setOf(cond("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardvark")) shouldBe true
    }

    @Test
    fun single_condition_which_is_false_for_case() {
        val rule = Rule(null, conclusion1, setOf(cond("vark")))
        rule.conditionsSatisfied(clinicalNotesCase("aardwolf")) shouldBe false
    }

    @Test
    fun any_condition_false_means_rule_does_not_apply() {
        val conditions = setOf(cond("a"), cond("b"), cond("c"), cond("d"))
        val rule = Rule(null, conclusion1, conditions)
        rule.conditionsSatisfied(clinicalNotesCase("abc")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("abd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cbd")) shouldBe false
        rule.conditionsSatisfied(clinicalNotesCase("cba")) shouldBe false
    }

    @Test
    fun rule_applies_if_all_true() {
        val conditions = setOf(cond("a"), cond("b"), cond("c"), cond("d"))
        val rule = Rule(null, conclusion1, conditions)
        rule.conditionsSatisfied(clinicalNotesCase("abcd")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("bcda")) shouldBe true
        rule.conditionsSatisfied(clinicalNotesCase("xdcba")) shouldBe true
    }

    @Test
    fun summary() {
        val conditions = setOf(cond("a"), cond("b"))
        val rule1 = Rule(null, null, conditions)
        rule1.summary().conclusion shouldBe null
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContain cond("a")
        rule1.summary().conditions shouldContain cond("b")

        val rule2 = Rule(null, conclusion1, conditions)
        rule2.summary().conclusion shouldBe conclusion1
        rule1.summary().conditions.size shouldBe 2
        rule1.summary().conditions shouldContain cond("a")
        rule1.summary().conditions shouldContain cond("b")
    }

    @Test
    fun rule_with_no_children_that_applies_to_case() {
        val conditions = setOf(cond("a"))
        val rule = Rule(null, conclusion1, conditions)
        val kase = clinicalNotesCase("ab")

        val result = rule.apply(kase, interpretation)
        result shouldBe true
        checkInterpretation(conclusion1)
    }

    //    @Test
//    fun `rule that does not apply to case and has no children`() {
//        val conditions = setOf(cond("a"))
//        val rule = Rule(null, conclusion1, conditions)
//        val kase = Kase("bc")
//
//        val result = rule.apply(kase, interpretation)
//        result shouldBe false
//        checkInterpretation()//empty
//    }
//
//    @Test
//    fun `rule applies to case but child does not`() {
//        val rule = setupRuleWithOneChild()
//        val kase = Kase("ac")
//
//        val result = rule.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion1)
//    }
//
//    @Test
//    fun `rule applies to case and so does child`() {
//        val conditions = setOf(cond("a"))
//        val rule = Rule(null, conclusion1, conditions)
//        val childConditions = setOf(cond("b"))
//        val childRule = Rule(null, conclusion2, childConditions)
//        rule.addChild(childRule)
//        val kase = Kase("ab")
//
//        val result = rule.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion2)
//    }
//
//    @Test
//    fun `rule does not apply to case but child does`() {
//        val rule = setupRuleWithOneChild()
//        val kase = Kase("bc")
//
//        val result = rule.apply(kase, interpretation)
//        result shouldBe false
//        checkInterpretation()//empty
//    }
//
//    @Test
//    fun `rule does not apply to case nor does child`() {
//        val rule = setupRuleWithOneChild()
//        val kase = Kase("xy")
//
//        val result = rule.apply(kase, interpretation)
//        result shouldBe false
//        checkInterpretation()//empty
//    }
//
//    @Test
//    fun `rule applies no child does`() {
//        val rule = setupRuleWithTwoChildren()
//        val kase = Kase("a")
//        val result = rule.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion1)
//    }
//
//    @Test
//    fun `rule applies and one child does`() {
//        val rule = setupRuleWithTwoChildren()
//        val kase = Kase("ab")
//        val result = rule.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion2)
//    }
//
//    @Test
//    fun `rule applies and so do both children`() {
//        val rule = setupRuleWithTwoChildren()
//        val kase = Kase("abc")
//        val result = rule.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion2, conclusion3)
//    }
//
//    @Test
//    fun addRuleTest() {
//        val grandChildConditions = setOf(cond("a"), cond("c"))
//        val grandChild = Rule(null, conclusion3, grandChildConditions)
//        val childConditions = setOf(cond("b"))
//        val childRule = Rule(null, conclusion2, childConditions)
//        childRule.addChild(grandChild)
//        childRule.conditions shouldEqual childRule.conditions
//        childRule.conclusion shouldEqual childRule.conclusion
//        val rootConditions = setOf(cond("a"), cond("b"))
//        val root = Rule(null, conclusion1, rootConditions)
//        root.addChild(childRule)
//        root.conclusion shouldEqual root.conclusion
//        root.conditions shouldEqual root.conditions
//        root.childRules() should contain(childRule)
//        val kase = Kase("abc")
//        val result = root.apply(kase, interpretation)
//        result shouldBe true
//        checkInterpretation(conclusion3)
//    }
//
//    @Test
//    fun visitTest() {
//        val conditions = setOf(cond("a"))
//        val rule = Rule(null, conclusion1, conditions)
//        val visited = mutableSetOf<Rule>()
//        val action: ((Rule) -> (Unit)) = {
//            visited.add(it)
//        }
//        rule.visit(action)
//        visited.size shouldEqual 1
//        visited should contain(rule)
//    }
//
//    @Test
//    fun `visit rule with children`() {
//        val rule = setupRuleWithTwoChildren()
//        val visited = mutableSetOf<Conclusion?>()
//        val action: ((Rule) -> (Unit)) = {
//            visited.add(it.conclusion)
//        }
//        rule.visit(action)
//        val expected = mutableSetOf(conclusion1, conclusion2, conclusion3)
//        visited shouldEqual expected
//    }
//
//    @Test
//    fun `visit deep`() {
//        val rule = setupRuleWithOneChild()
//        val grandChildConditions = setOf(cond("a"), cond("c"))
//        val grandChild = Rule(null, conclusion3, grandChildConditions)
//        rule.childRules().first().addChild(grandChild)
//
//        val visited = mutableSetOf<Conclusion?>()
//        val action: ((Rule) -> (Unit)) = {
//            visited.add(it.conclusion)
//        }
//        rule.visit(action)
//        val expected = mutableSetOf(conclusion1, conclusion2, conclusion3)
//        visited shouldEqual expected
//    }
//
//    @Test
//    fun `rule should be copied`() {
//        val rule = setupRuleWithOneChild()
//        val copy = rule.copy()
//        (copy !== rule) shouldBe true
//        copy.conclusion shouldBe rule.conclusion
//        copy.conditions shouldEqual rule.conditions
//        copy.childRules() shouldEqual rule.childRules()
//    }
//
//    @Test
//    fun `rule with null parent should be copied`() {
//        val rule = setupRuleWithOneChild()
//        val copy = rule.copy()
//        (copy !== rule) shouldBe true
//        copy.parent shouldBe rule.parent
//        copy.conclusion shouldBe rule.conclusion
//        copy.conditions shouldEqual rule.conditions
//        copy.childRules() shouldEqual rule.childRules()
//    }
//
//    @Test
//    fun `rule with not null  parent should be copied`() {
//        val rule = setupRuleWithOneChild()
//        rule.parent = Rule(null)
//        val copy = rule.copy()
//        (copy !== rule) shouldBe true
//        copy.parent shouldBe Rule(null)
//        copy.conclusion shouldBe rule.conclusion
//        copy.conditions shouldEqual rule.conditions
//        copy.childRules() shouldEqual rule.childRules()
//    }
//    @Test
//    fun `child rules should be copied`() {
//        val rule = setupRuleWithOneChild()
//        val copy = rule.copy()
//        val copyChild = copy.childRules().iterator().next()
//        val ruleChild = rule.childRules().iterator().next()
//        copyChild shouldEqual ruleChild
//        (copyChild !== ruleChild) shouldBe true
//    }
//
//    @Test
//    fun `conditions should be copied`() {
//        val rule = setupRuleWithOneChild()
//        val copy = rule.copy()
//        val copyCondition = copy.conditions.iterator().next()
//        val ruleCondition = rule.conditions.iterator().next()
//        copyCondition shouldBe ruleCondition
//    }
//
//    private fun setupRuleWithTwoChildren(): Rule {
//        val rule = setupRuleWithOneChild()
//        val childConditions = setOf(cond("c"))
//        val childRule = Rule(null, conclusion3, childConditions)
//        rule.addChild(childRule)
//        return rule
//    }
//
//    private fun setupRuleWithOneChild(): Rule {
//        val rule = Rule(null, conclusion1, setOf(cond("a")))
//        val childRule = Rule(null, conclusion2, setOf(cond("b")))
//        rule.addChild(childRule)
//        return rule
//    }
//
    private fun checkInterpretation(vararg conclusions: Conclusion) {
        checkInterpretation(interpretation, *conclusions)
    }

    fun checkInterpretation(interpretation: Interpretation, vararg conclusions: Conclusion) {
        conclusions.size shouldBe interpretation.conclusions().size
        conclusions.forEach {
            interpretation.conclusions() shouldContain it
        }
    }
}