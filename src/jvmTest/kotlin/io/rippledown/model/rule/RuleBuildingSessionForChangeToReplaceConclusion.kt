package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToReplaceConclusion : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstones = mutableSetOf(cc1,cc2)

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToReplaceConclusion(Conclusion("Whatever"), Conclusion("Blah"))
        addAction.toString() shouldBe "ChangeTreeToReplaceConclusion(toBeReplaced=Conclusion(text=Whatever) replacement=Conclusion(text=Blah))"
    }

    @Test
    fun a_session_for_a_replace_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion("A"), Conclusion("D"))
        val ruleGivingA = Rule("ra", null, Conclusion("A"))
        val ruleGivingB = Rule("rb", null, Conclusion("B"))
        val ruleGivingC = Rule("rc", null, Conclusion("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(tree, sessionCase, replaceAction, cornerstones)
        val condition = ContainsText(clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_replace_action_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule("ra", null, Conclusion("A"))
        val ruleGivingB = Rule("rb", null, Conclusion("B"))
        val ruleGivingC = Rule("rc", null, Conclusion("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val replaceAction = ChangeTreeToReplaceConclusion(Conclusion("A"), Conclusion("B"))
        val session = RuleBuildingSession(tree, sessionCase, replaceAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_replace_action_should_add_the_rule_under_the_rule_corresponding_to_the_conclusion_to_be_replaced() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +"B"
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val action = ChangeTreeToReplaceConclusion(Conclusion("A"), Conclusion("B"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(tree, case, action, setOf())
            .addCondition(ContainsText(clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldContainExactly setOf(ContainsText(clinicalNotes, "a"))
        ruleAdded.conclusion!!.text shouldBe "B"
        ruleAdded.parent!!.conclusion!!.text shouldBe "A"
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +"B"
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        val action = ChangeTreeToReplaceConclusion(Conclusion("A"), Conclusion("B"))
        val case = clinicalNotesCase("c")
        val caseA = clinicalNotesCase("a")
        val caseB = clinicalNotesCase("b")
        val caseAB = clinicalNotesCase("ab")
        action.isApplicable(tree, case) shouldBe false
        action.isApplicable(tree, caseA) shouldBe true
        action.isApplicable(tree, caseB) shouldBe false
        action.isApplicable(tree, caseAB) shouldBe true
    }
}