package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Conclusion
import kotlin.test.Test


internal class NoConclusionRuleTest : RuleTestBase() {

    @Test
    fun stopping_rule_that_applies_to_case_should_not_add_conclusions() {
        val rule = NoConclusionRule(setOf(cond("a")))
        val result = rule.apply(clinicalNotesCase("ab"), interpretation)
        result shouldBe true
        interpretation.conclusions() shouldBe setOf()
    }

    @Test
    fun stopping_rule_that_does_not_apply_to_case_should_evaluate_false() {
        val rule = NoConclusionRule(setOf(cond("a")))
        val result = rule.apply(clinicalNotesCase("bc"), interpretation)
        result shouldBe false
        interpretation.conclusions() shouldBe setOf<Conclusion>()
    }

    @Test
    fun stopping_rule_should_have_no_children() {
        val rule = NoConclusionRule(setOf(cond("a")))
        rule.childRules().size shouldBe 0
    }

    @Test
    fun stopping_rules_are_not_equal_if_conditions_are_different() {
        val rule1 = NoConclusionRule(setOf(cond("a")))
        val rule2 = NoConclusionRule(setOf(cond("b")))
        rule1 shouldNotBe rule2
    }

    @Test
    fun stopping_rule_has_an_effect_if_it_is_given() {
        val parent = Rule(null, conc("A"), setOf(cond("a")))
        val stoppingRule = NoConclusionRule(setOf(cond("b")))
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(stoppingRule)

        //stopping rule is given
        val result2 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result2 shouldBe true
        interpretation.conclusions() shouldBe setOf<Conclusion>()
    }

    @Test
    fun stopping_rule_has_no_effect_if_it_is_not_given() {
        val conclusion = conc("A")
        val parent = Rule(null, conclusion, setOf(cond("a")))
        val stoppingRule = NoConclusionRule(setOf(cond("b")))
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(stoppingRule)

        //stopping rule is not given
        val result1 = parent.apply(clinicalNotesCase("a"), interpretation)
        result1 shouldBe true
        interpretation.conclusions() shouldBe setOf(conclusion)
    }

    @Test
    fun stopping_rule_can_be_a_sibling() {
        val conclusionA = conc("A")
        val conclusionB = conc("B")
        val parent = Rule(null, conclusionA, setOf(cond("a")))
        val childRule = Rule(null, conclusionB, setOf(cond("b")))
        val stoppingRule = NoConclusionRule(setOf(cond("c")))
        parent.addChild(childRule)
        parent.addChild(stoppingRule)
        parent.childRules() shouldBe setOf(childRule, stoppingRule)

        //non-stopping child rule only is given
        val result1 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result1 shouldBe true
        interpretation.conclusions() shouldBe setOf<Conclusion>(conclusionB)

        //both child rules are given
        val result2 = parent.apply(clinicalNotesCase("ab"), interpretation)
        result2 shouldBe true
        interpretation.conclusions() shouldBe setOf<Conclusion>(conclusionB)
    }

    @Test
    fun copy_stopping_rule() {
        val conditions = setOf(cond("a"))
        val rule = NoConclusionRule(conditions)
        val copy = rule.copy()
        copy.conditions shouldBe rule.conditions
        copy.childRules() shouldBe rule.childRules()
        (copy === rule) shouldBe false
    }

    @Test
    fun test_equals() {
        val conclusionA = conc("A")
        val parent = Rule(null, conclusionA, setOf(cond("a")))
        val conditions = setOf(cond("b"))
        val stopping1 = NoConclusionRule(conditions)
        val stopping2 = NoConclusionRule(conditions)
        stopping1.parent = parent
        stopping2.parent = parent
        stopping1 shouldBe stopping2
        stopping1 shouldBe stopping1
        stopping1 shouldNotBe parent
    }

    @Test
    fun equals_depends_on_conditions() {
        val parent = Rule(null, conc("A"), setOf(cond("a")))
        val stopping1 = NoConclusionRule(setOf(cond("b")))
        val stopping2 = NoConclusionRule(setOf(cond("c")))
        stopping1.parent = parent
        stopping2.parent = parent
        stopping1 shouldNotBe stopping2
    }

    @Test
    fun equals_depends_on_parent() {
        val parent1 = Rule(null, conc("A"), setOf(cond("a")))
        val parent2 = Rule(null, conc("B"), setOf(cond("a")))
        val stopping1 = NoConclusionRule(setOf(cond("b")))
        val stopping2 = NoConclusionRule(setOf(cond("b")))
        stopping1.parent = parent1
        stopping2.parent = parent2
        stopping1 shouldNotBe stopping2
    }
}