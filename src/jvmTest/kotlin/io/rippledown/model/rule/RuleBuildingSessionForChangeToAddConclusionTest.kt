package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToAddConclusionTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstoneMap = mutableSetOf(cc1, cc2)

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("Whatever"))
        addAction.toString() shouldBe "ChangeTreeToAddConclusion(toBeAdded=Conclusion(text=Whatever))"
    }

    @Test
    fun a_session_for_an_add_action_should_present_all_cornerstones_if_there_are_no_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(RuleTree(), sessionCase, addAction, cornerstoneMap)
        session.cornerstoneCases() shouldBe cornerstoneMap
    }

    @Test
    fun a_session_for_an_add_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(RuleTree(), sessionCase, addAction, cornerstoneMap)
        val condition = ContainsText(clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_an_add_action_should_present_no_cornerstones_if_none_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(RuleTree(), sessionCase, addAction, cornerstoneMap)
        val condition = ContainsText(clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptySet()
    }

    @Test
    fun removing_a_condition_should_mean_that_the_corresponding_cornerstones_are_now_presented() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(RuleTree(), sessionCase, addAction, cornerstoneMap)
        session.cornerstoneCases() shouldBe cornerstoneMap
        val condition = ContainsText(clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptySet()
        session.removeCondition(condition)
        session.cornerstoneCases() shouldBe cornerstoneMap
    }

    @Test
    fun exempting_a_cornerstone_should_mean_that_it_is_no_longer_presented() {
        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(RuleTree(), sessionCase, addAction, cornerstoneMap)
        session.exemptCornerstone(cc1)
        session.cornerstoneCases() shouldBe setOf(cc2)
    }

    @Test
    fun updating_the_rule_tree_for_an_add_action_should_add_the_rule_under_the_root() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "1"
                }
            }
            child {
                +"B"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "3"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))
        val session = RuleBuildingSession(tree, sessionCase, addAction, setOf())
        session
            .addCondition(ContainsText(clinicalNotes, "3"))
            .addCondition(ContainsText(clinicalNotes, "1"))
            .commit()

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldContainExactly setOf(ContainsText(clinicalNotes, "3"), ContainsText(clinicalNotes, "1"))
        ruleAdded.conclusion shouldBe Conclusion("A")
        ruleAdded.parent!!.parent shouldBe null
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree {
            child {
                +"A"
                condition {
                    attributeName = clinicalNotes.name
                    constant = "1"
                }
            }
        }.build()

        val addAction = ChangeTreeToAddConclusion(Conclusion("A"))

        val hasConclusionAlready = clinicalNotesCase("1")
        addAction.isApplicable(tree, hasConclusionAlready) shouldBe false

        val doesNotHaveConclusionAlready = clinicalNotesCase("2")
        addAction.isApplicable(tree, doesNotHaveConclusionAlready) shouldBe true
    }
}
