package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldBeEqualUsingSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToRemoveConclusionTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstones = mutableSetOf(cc1, cc2)
    private val ruleFactory = DummyRuleFactory()
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var conditionFactory: DummyConditionFactory

    @BeforeTest
    fun setup() {
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToRemoveConclusion(Conclusion(4, "Whatever"))
        addAction.toString() shouldBe "ChangeTreeToRemoveConclusion(toBeRemoved=Conclusion(id=4, text=Whatever))"
    }

    @Test
    fun a_session_for_a_remove_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val conclusionA = conclusionFactory.getOrCreate("A")
        val removeAction = ChangeTreeToRemoveConclusion(conclusionA)
        val ruleGivingA = Rule(5,null, conclusionA)
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule(6,null, conclusionFactory.getOrCreate("B"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule(7,null , conclusionFactory.getOrCreate("C"))
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(ruleFactory, tree, sessionCase,  removeAction, cornerstones)
        val condition = ContainsText(null, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_remove_action_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule(7, null, conclusionFactory.getOrCreate("A"))
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule(8, null, conclusionFactory.getOrCreate("b"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule(8, null, conclusionFactory.getOrCreate("C"))
        tree.root.addChild(ruleGivingC)

        val removeAction = ChangeTreeToRemoveConclusion(conclusionFactory.getOrCreate("A"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, removeAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_remove_action_should_add_the_rule_under_the_rule_to_be_stopped() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +"B"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val removeAction = ChangeTreeToRemoveConclusion(conclusionFactory.getOrCreate("A"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(ruleFactory, tree, case,  removeAction, setOf())
            .addCondition(ContainsText(null, clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(ContainsText(null, clinicalNotes, "a"))
        ruleAdded.conclusion shouldBe null
        ruleAdded.parent!!.conclusion!!.text shouldBe "A"
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()

        val removeAction = ChangeTreeToRemoveConclusion(conclusionFactory.getOrCreate("A"))
        val caseWithConclusion = clinicalNotesCase("a")
        removeAction.isApplicable(tree, caseWithConclusion) shouldBe true

        val caseWithoutConclusion = clinicalNotesCase("b")
        removeAction.isApplicable(tree, caseWithoutConclusion) shouldBe false
    }
}