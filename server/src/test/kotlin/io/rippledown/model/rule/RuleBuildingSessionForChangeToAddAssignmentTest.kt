package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldBeEqualUsingSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToAddAssignmentTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstonesList = mutableListOf(cc1, cc2)
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: DummyConditionFactory
    private val ruleFactory = DummyRuleFactory()

    @BeforeTest
    fun setup() {
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun toStringTest() {
        val addAction = ChangeTreeToAddAssignment(comment("Whatever"))
        addAction.toString() shouldBe "ChangeTreeToAddAssignment(toBeAdded=AssignValue(attribute=Attribute(id=1000, name=C1, kind=COMMENT), expression=CommentTemplate(text=Whatever, variables=[])))"
    }

    @Test
    fun a_session_for_an_ADD_COMMENT_should_present_all_cornerstones_if_there_are_no_conditions() {
        val addAction = ChangeTreeToAddAssignment(comment("A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.cornerstoneCases() shouldBe cornerstonesList
    }

    @Test
    fun a_session_for_an_ADD_COMMENT_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddAssignment(comment("A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        val condition = containsText(null, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_an_ADD_COMMENT_should_present_no_cornerstones_if_none_satisfy_the_conditions() {
        val addAction = ChangeTreeToAddAssignment(comment("A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        val condition = containsText(null, clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptyList()
    }

    @Test
    fun removing_a_condition_should_mean_that_the_corresponding_cornerstones_are_now_presented() {
        val addAction = ChangeTreeToAddAssignment(comment("A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.cornerstoneCases() shouldBe cornerstonesList
        val condition = containsText(null, clinicalNotes, "3")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe emptyList()
        session.removeCondition(condition)
        session.cornerstoneCases() shouldBe cornerstonesList
    }

    @Test
    fun exempting_a_cornerstone_should_mean_that_it_is_no_longer_presented() {
        val addAction = ChangeTreeToAddAssignment(comment("A"))
        val session = RuleBuildingSession(ruleFactory, RuleTree(), sessionCase, addAction, cornerstonesList)
        session.exemptCornerstone(cc1)
        session.cornerstoneCases() shouldBe setOf(cc2)
    }

    @Test
    fun updating_the_rule_tree_for_an_ADD_COMMENT_should_add_the_rule_under_the_root() {
        val tree = ruleTree(commentFactory) {
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

        val addAction = ChangeTreeToAddAssignment(commentFactory.comment("A"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, addAction, listOf())
        session
            .addCondition(containsText(null, clinicalNotes, "3"))
            .addCondition(containsText(null, clinicalNotes, "1"))
            .commit()

        tree.root.childRules().size shouldBe 3
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(containsText(null, clinicalNotes, "3"), containsText(null, clinicalNotes, "1"))
        ruleAdded.assignment shouldBe commentFactory.comment("A")
        ruleAdded.parent!!.parent shouldBe null
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree(commentFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "1"
                }
            }
        }.build()

        val addAction = ChangeTreeToAddAssignment(commentFactory.comment("A"))

        val hasAssignmentAlready = clinicalNotesCase("1")
        addAction.isApplicable(tree, hasAssignmentAlready) shouldBe false

        val doesNotHaveAssignmentAlready = clinicalNotesCase("2")
        addAction.isApplicable(tree, doesNotHaveAssignmentAlready) shouldBe true
    }
}
