package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldBeEqualUsingSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToReplaceConclusion : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstones = mutableSetOf(cc1,cc2)
    private val ruleFactory = DummyRuleFactory()
    private lateinit var conclusionFactory: DummyConclusionFactory

    @BeforeTest
    fun setup() {
        conclusionFactory = DummyConclusionFactory()
    }

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToReplaceConclusion(Conclusion(4, "Whatever"), Conclusion(5, "Blah"))
        addAction.toString() shouldBe "ChangeTreeToReplaceConclusion(toBeReplaced=Conclusion(id=4, text=Whatever) replacement=Conclusion(id=5, text=Blah))"
    }

    @Test
    fun a_session_for_a_replace_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val conclusionA = conclusionFactory.getOrCreate("A")
        val replaceAction = ChangeTreeToReplaceConclusion(conclusionA, conclusionFactory.getOrCreate("D"))
        val ruleGivingA = Rule(5, null, conclusionA)
        val ruleGivingB = Rule(6, null, conclusionFactory.getOrCreate("B"))
        val ruleGivingC = Rule(6, null, conclusionFactory.getOrCreate("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, replaceAction, cornerstones)
        val condition = ContainsText(100, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_replace_action_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule(2, null, conclusionFactory.getOrCreate("A"))
        val ruleGivingB = Rule(3, null, conclusionFactory.getOrCreate("B"))
        val ruleGivingC = Rule(4, null, conclusionFactory.getOrCreate("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val replaceAction = ChangeTreeToReplaceConclusion(conclusionFactory.getOrCreate("A"), conclusionFactory.getOrCreate("B"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, replaceAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_replace_action_should_add_the_rule_under_the_rule_corresponding_to_the_conclusion_to_be_replaced() {
        val tree = ruleTree(conclusionFactory) {
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

        val action = ChangeTreeToReplaceConclusion(conclusionFactory.getOrCreate("A"), conclusionFactory.getOrCreate("B"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(ruleFactory, tree, case, action, setOf())
            .addCondition(ContainsText(null, clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(ContainsText(null, clinicalNotes, "a"))
        ruleAdded.conclusion!!.text shouldBe "B"
        ruleAdded.parent!!.conclusion!!.text shouldBe "A"
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree(conclusionFactory) {
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

        val action = ChangeTreeToReplaceConclusion(conclusionFactory.getOrCreate("A"), conclusionFactory.getOrCreate("B"))
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