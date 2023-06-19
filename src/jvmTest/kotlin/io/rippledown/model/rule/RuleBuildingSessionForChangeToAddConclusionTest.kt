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

internal class RuleBuildingSessionForChangeToAddConclusionTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstonesList = mutableListOf(cc1, cc2)
    private lateinit var conclusionFactory: DummyConclusionFactory
    private lateinit var conditionFactory: DummyConditionFactory
    private val ruleFactory = DummyRuleFactory()

    @BeforeTest
    fun setup() {
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(1, "Whatever"))
        addAction.toString() shouldBe "ChangeTreeToAddConclusion(toBeAdded=Conclusion(id=1, text=Whatever))"
    }

    @Test
    fun a_session_for_an_add_action_should_present_all_cornerstones_if_there_are_no_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(1, "A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.cornerstoneCases() shouldBe cornerstonesList
    }

    @Test
    fun a_session_for_an_add_action_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(1,"A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        val condition = ContainsText(null, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_an_add_action_should_present_no_cornerstones_if_none_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(1, "A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        val condition = ContainsText(null, clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptySet()
    }

    @Test
    fun removing_a_condition_should_mean_that_the_corresponding_cornerstones_are_now_presented() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(2, "A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.cornerstoneCases() shouldBe cornerstonesList
        val condition = ContainsText(null, clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptySet()
        session.removeCondition(condition)
        session.cornerstoneCases() shouldBe cornerstonesList
    }

    @Test
    fun exempting_a_cornerstone_should_mean_that_it_is_no_longer_presented() {
        val addAction = ChangeTreeToAddConclusion(Conclusion(2, "A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.exemptCornerstone(cc1)
        session.cornerstoneCases() shouldBe setOf(cc2)
    }

    @Test
    fun updating_the_rule_tree_for_an_add_action_should_add_the_rule_under_the_root() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "1"
                }
            }
            child {
                +"B"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "3"
                }
            }
        }.build()

        tree.root.childRules().size shouldBe 2 //sanity
        val rulesBefore = tree.rules()

        val addAction = ChangeTreeToAddConclusion(conclusionFactory.getOrCreate("A"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, addAction, listOf())
        session
            .addCondition(ContainsText(null, clinicalNotes, "3"))
            .addCondition(ContainsText(null, clinicalNotes, "1"))
            .commit()

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(ContainsText(null, clinicalNotes, "3"), ContainsText(null, clinicalNotes, "1"))
        ruleAdded.conclusion shouldBe conclusionFactory.getOrCreate("A")
        ruleAdded.parent!!.parent shouldBe null
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree(conclusionFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "1"
                }
            }
        }.build()

        val addAction = ChangeTreeToAddConclusion(conclusionFactory.getOrCreate("A"))

        val hasConclusionAlready = clinicalNotesCase("1")
        addAction.isApplicable(tree, hasConclusionAlready) shouldBe false

        val doesNotHaveConclusionAlready = clinicalNotesCase("2")
        addAction.isApplicable(tree, doesNotHaveConclusionAlready) shouldBe true
    }
}
