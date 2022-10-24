package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToRemoveConclusionTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstones = mutableSetOf(cc1, cc2)

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToRemoveConclusion(Conclusion("Whatever"))
        addAction.toString() shouldBe "ChangeTreeToRemoveConclusion(toBeRemoved=Conclusion(text=Whatever))"
    }

    @Test
    fun a_session_for_a_remove_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"))
        val ruleGivingA = Rule("ra",null, Conclusion("A"))
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule("rb",null, Conclusion("B"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule("rc",null, Conclusion("C"))
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(tree, sessionCase,  removeAction, cornerstones)
        val condition = ContainsText(clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_remove_action_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule("ra", null, Conclusion("A"))
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule("rb", null, Conclusion("B"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule("rc", null, Conclusion("C"))
        tree.root.addChild(ruleGivingC)

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"))
        val session = RuleBuildingSession(tree, sessionCase, removeAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_remove_action_should_add_the_rule_under_the_rule_to_be_stopped() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "a"
                }
            }
            child {
                +"B"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(tree, case,  removeAction, setOf())
            .addCondition(ContainsText(clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldContainExactly setOf(ContainsText(clinicalNotes, "a"))
        ruleAdded.conclusion shouldBe null
        ruleAdded.parent!!.conclusion!!.text shouldBe "A"
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "a"
                }
            }
        }.build()

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"))
        val caseWithConclusion = clinicalNotesCase("a")
        removeAction.isApplicable(tree, caseWithConclusion) shouldBe true

        val caseWithoutConclusion = clinicalNotesCase("b")
        removeAction.isApplicable(tree, caseWithoutConclusion) shouldBe false
    }
}