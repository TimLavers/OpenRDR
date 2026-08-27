package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldBeEqualUsingSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToRemoveAssignmentTest : RuleTestBase() {
    private val sessionCase = clinicalNotesCase("123")
    private val cc1 = clinicalNotesCase("CC1")
    private val cc2 = clinicalNotesCase("CC2")
    private val cornerstones = mutableListOf(cc1, cc2)
    private val ruleFactory = DummyRuleFactory()
    private lateinit var commentFactory: CommentFactory
    private lateinit var conditionFactory: DummyConditionFactory

    @BeforeTest
    fun setup() {
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
    }

    @Test
    fun toStringTest() {
        val removeAction = ChangeTreeToRemoveAssignment(comment("Whatever"))
        removeAction.toString() shouldBe "ChangeTreeToRemoveAssignment(toBeRemoved=AssignValue(attribute=Attribute(id=1000, name=C1, kind=COMMENT), expression=CommentTemplate(text=Whatever, variables=[])))"
    }

    @Test
    fun a_session_for_a_REMOVE_COMMENT_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val commentA = commentFactory.comment("A")
        val removeAction = ChangeTreeToRemoveAssignment(commentA)
        val ruleGivingA = Rule(5, null, mutableSetOf(), mutableSetOf(), commentA)
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule(6, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("B"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule(7, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("C"))
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(ruleFactory, tree, sessionCase,  removeAction, cornerstones)
        val condition = containsText(null, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_REMOVE_COMMENT_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule(7, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("A"))
        tree.root.addChild(ruleGivingA)
        val ruleGivingB = Rule(8, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("b"))
        tree.root.addChild(ruleGivingB)
        val ruleGivingC = Rule(8, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("C"))
        tree.root.addChild(ruleGivingC)

        val removeAction = ChangeTreeToRemoveAssignment(commentFactory.comment("A"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, removeAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_REMOVE_COMMENT_should_add_the_rule_under_the_rule_to_be_stopped() {
        val tree = ruleTree(commentFactory) {
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

        val removeAction = ChangeTreeToRemoveAssignment(commentFactory.comment("A"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(ruleFactory, tree, case, removeAction, listOf())
            .addCondition(containsText(null, clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(containsText(null, clinicalNotes, "a"))
        ruleAdded.assignment shouldBe null
        (ruleAdded.parent!!.assignment!!.expression as CommentTemplate).text shouldBe "A"
    }

    @Test
    fun isApplicable() {
        val tree = ruleTree(commentFactory) {
            child {
                +"A"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
        }.build()

        val removeAction = ChangeTreeToRemoveAssignment(commentFactory.comment("A"))
        val caseWithAssignment = clinicalNotesCase("a")
        removeAction.isApplicable(tree, caseWithAssignment) shouldBe true

        val caseWithoutAssignment = clinicalNotesCase("b")
        removeAction.isApplicable(tree, caseWithoutAssignment) shouldBe false
    }
}