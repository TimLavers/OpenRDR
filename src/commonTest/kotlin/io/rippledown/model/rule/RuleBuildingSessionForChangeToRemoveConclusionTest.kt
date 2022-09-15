package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToRemoveConclusionTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val interpretationA = Interpretation(CaseId("A", "A"))
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val interp1 = Interpretation(CaseId("CC1", "CC1"))
    private val interp2 = Interpretation(CaseId("CC2", "CC2"))
    private val cornerstoneMap = mutableMapOf(Pair(cc1, interp1), Pair(cc2, interp2))

    @Test
    fun a_session_for_a_remove_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"), RuleTree())
        val ruleGivingA = Rule("ra",null, Conclusion("A"))
        val ruleGivingB = Rule("rb",null, Conclusion("B"))
        val ruleGivingC = Rule("rc",null, Conclusion("C"))
        val i1 = Interpretation(interp1.caseId, "")
        i1.add(ruleGivingA)
        i1.add(ruleGivingB)
        val i2 = Interpretation(interp2.caseId, "")
        i2.add(ruleGivingA)
        i2.add(ruleGivingC)

        cornerstoneMap[cc1] = i1
        cornerstoneMap[cc2] = i2
        val session = RuleBuildingSession(sessionCase, interpretationA, removeAction, cornerstoneMap)
        val condition = ContainsText(clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun A_session_for_a_remove_action_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val ruleGivingA = Rule("ra", null, Conclusion("A"))
        val ruleGivingB = Rule("rb", null, Conclusion("B"))
        val ruleGivingC = Rule("rc", null, Conclusion("C"))
        val i1 = Interpretation(interp1.caseId, "")
        i1.add(ruleGivingA)
        i1.add(ruleGivingB)
        val i2 = Interpretation(interp2.caseId, "")
        i2.add(ruleGivingA)
        i2.add(ruleGivingC)

        cornerstoneMap[cc1] = i1
        cornerstoneMap[cc2] = i2
        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"), RuleTree())
        val session = RuleBuildingSession(sessionCase, interpretationA, removeAction, cornerstoneMap)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)

        val removeAction2 = ChangeTreeToRemoveConclusion(Conclusion("B"), RuleTree())
        val session2 = RuleBuildingSession(sessionCase, interpretationA, removeAction2, cornerstoneMap)
        session2.cornerstoneCases() shouldBe setOf(cc1)

        val removeAction3 = ChangeTreeToRemoveConclusion(Conclusion("C"), RuleTree())
        val session3 = RuleBuildingSession(sessionCase, interpretationA, removeAction3, cornerstoneMap)
        session3.cornerstoneCases() shouldBe setOf(cc2)
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

        val removeAction = ChangeTreeToRemoveConclusion(Conclusion("A"), tree)
        val case = clinicalNotesCase("a")
        RuleBuildingSession(case, interpretationA, removeAction, mapOf())
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
}