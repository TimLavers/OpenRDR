package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

internal class StoppingRuleTest : RuleTestBase() {

    @Test
    fun stopping_rule_that_applies_to_case_should_not_make_an_assignment() {
        val rule = Rule(9, null, setOf(createCondition("a")))
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        interpretation.assignments() shouldBe setOf()
    }

    @Test
    fun stopping_rule_that_does_not_apply_to_case_should_evaluate_false() {
        val rule = Rule(9, null, setOf(createCondition("a")))
        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        interpretation.assignments() shouldBe setOf()
    }

    @Test
    fun stopping_rule_should_have_no_children() {
        val rule = Rule(9, null, setOf(createCondition("a")))
        rule.childRules().size shouldBe 0
    }

    @Test
    fun stopping_rules_are_not_equal_if_conditions_are_different() {
        val rule1 = Rule(9, null, setOf(createCondition("a")))
        val rule2 = Rule(9, null, setOf(createCondition("b")))
        rule1.structurallyEqual(rule2) shouldBe false
    }

    @Test
    fun stopping_rule_has_an_effect_if_it_is_given() {
        val parent = Rule(8, null, setOf(createCondition("a")), mutableSetOf(), comment("A"))
        val stoppingRule = Rule(9, null, setOf(createCondition("b")))
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(stoppingRule)

        //stopping rule is given
        val result2 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result2 shouldBe true
        interpretation.assignments() shouldBe setOf()
    }

    @Test
    fun stopping_rule_has_no_effect_if_it_is_not_given() {
        val commentA = comment("A")
        val parent = Rule(7, null, setOf(createCondition("a")), mutableSetOf(), commentA)
        val stoppingRule = Rule(9, null, setOf(createCondition("b")))
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(stoppingRule)

        //stopping rule is not given
        val result1 = parent.apply(clinicalNotesCase("a"), interpretation)
        result1 shouldBe true
        interpretation.assignments() shouldBe setOf(commentA)
    }

    @Test
    fun stopping_rule_can_be_a_sibling() {
        val commentA = comment("A")
        val commentB = comment("B")
        val parent = Rule(8, null, setOf(createCondition("a")), mutableSetOf(), commentA)
        val childRule = Rule(3, null, setOf(createCondition("b")), mutableSetOf(), commentB)
        val stoppingRule = Rule(78, null, setOf(createCondition("c")))
        parent.addChild(childRule)
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(childRule, stoppingRule)

        //non-stopping child rule only is given
        val result1 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result1 shouldBe true
        interpretation.assignments() shouldBe setOf(commentB)

        //both child rules are given
        val result2 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result2 shouldBe true
        interpretation.assignments() shouldBe setOf(commentB)
    }

    @Test
    fun copy_stopping_rule() {
        val conditions = setOf(createCondition("a"))
        val rule = Rule(9, null, conditions)
        val copy = rule.copy()
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
        (copy === rule) shouldBe false
    }

    @Test
    fun test_equals() {
        val parent = Rule(7, null, setOf(createCondition("a")), mutableSetOf(), comment("A"))
        val conditions = setOf(createCondition("b"))
        val stopping1 = Rule(78, null, conditions)
        val stopping2 = Rule(79, null, conditions)
        stopping1.parent = parent
        stopping2.parent = parent
        stopping1.structurallyEqual(stopping2) shouldBe true
        stopping2.structurallyEqual(stopping1) shouldBe true

        stopping1 shouldNotBe stopping2
        stopping1 shouldNotBe parent
    }

    @Test
    fun equals_depends_on_conditions() {
        val parent = Rule(8, null, setOf(createCondition("a")), mutableSetOf(), comment("A"))
        val stopping1 = Rule(9, null, setOf(createCondition("b")))
        val stopping2 = Rule(9, null, setOf(createCondition("c")))
        stopping1.parent = parent
        stopping2.parent = parent
        stopping1 shouldBe stopping2
        stopping1.structurallyEqual(stopping2) shouldBe false
        stopping2.structurallyEqual(stopping1) shouldBe false
    }

    @Test
    fun equals_depends_on_parent() {
        val parent1 = Rule(11, null, setOf(createCondition("a")), mutableSetOf(), comment("A"))
        val parent2 = Rule(12, null, setOf(createCondition("a")), mutableSetOf(), comment("B"))
        val stopping1 = Rule(21, null, setOf(createCondition("b")))
        val stopping2 = Rule(22, null, setOf(createCondition("b")))
        stopping1.parent = parent1
        stopping2.parent = parent2
        stopping1 shouldNotBe stopping2
        stopping1.structurallyEqual(stopping2) shouldBe false
        stopping2.structurallyEqual(stopping1) shouldBe false
    }
}