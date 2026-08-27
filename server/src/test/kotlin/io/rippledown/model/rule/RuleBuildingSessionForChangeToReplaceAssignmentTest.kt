package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.condition.containsText
import io.rippledown.model.rule.dsl.ruleTree
import io.rippledown.util.shouldBeEqualUsingSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RuleBuildingSessionForChangeToReplaceAssignmentTest : RuleTestBase() {
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
        val replaceAction = ChangeTreeToReplaceAssignment(comment("Whatever"), comment("Blah"))
        replaceAction.toString() shouldBe "ChangeTreeToReplaceAssignment(toBeReplaced=AssignValue(attribute=Attribute(id=1000, name=C1, kind=COMMENT), expression=CommentTemplate(text=Whatever, variables=[])) replacement=AssignValue(attribute=Attribute(id=1001, name=C2, kind=COMMENT), expression=CommentTemplate(text=Blah, variables=[])))"
    }

    @Test
    fun a_session_for_a_REPLACE_COMMENT_should_present_those_cornerstones_which_satisfy_the_conditions() {
        val tree = RuleTree()
        val commentA = commentFactory.comment("A")
        val replaceAction = ChangeTreeToReplaceAssignment(commentA, commentFactory.comment("D"))
        val ruleGivingA = Rule(5, null, mutableSetOf(), mutableSetOf(), commentA)
        val ruleGivingB = Rule(6, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("B"))
        val ruleGivingC = Rule(6, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, replaceAction, cornerstones)
        val condition = containsText(100, clinicalNotes, "1")
        session.addCondition(condition)
        session.cornerstoneCases() shouldBe setOf(cc1)
    }

    @Test
    fun a_session_for_a_REPLACE_COMMENT_should_only_present_those_cornerstones_whose_interpretations_would_change() {
        val tree = RuleTree()
        val ruleGivingA = Rule(2, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("A"))
        val ruleGivingB = Rule(3, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("B"))
        val ruleGivingC = Rule(4, null, mutableSetOf(), mutableSetOf(), commentFactory.comment("C"))
        tree.root.addChild(ruleGivingA)
        tree.root.addChild(ruleGivingB)
        tree.root.addChild(ruleGivingC)

        val replaceAction = ChangeTreeToReplaceAssignment(commentFactory.comment("A"), commentFactory.comment("B"))
        val session = RuleBuildingSession(ruleFactory, tree, sessionCase, replaceAction, cornerstones)
        session.cornerstoneCases() shouldBe setOf(cc1, cc2)
    }

    @Test
    fun updating_the_rule_tree_for_a_REPLACE_COMMENT_should_add_the_rule_under_the_rule_corresponding_to_the_conclusion_to_be_replaced() {
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

        val action = ChangeTreeToReplaceAssignment(commentFactory.comment("A"), commentFactory.comment("B"))
        val case = clinicalNotesCase("a")
        RuleBuildingSession(ruleFactory, tree, case, action, listOf())
            .addCondition(containsText(null, clinicalNotes, "a"))
            .commit()

        tree.root.childRules().size shouldBe 2
        val rulesAdded = tree.rules().minus(rulesBefore)
        rulesAdded.size shouldBe 1
        val ruleAdded = rulesAdded.random()
        ruleAdded.childRules() shouldBe emptySet()
        ruleAdded.conditions shouldBeEqualUsingSameAs setOf(containsText(null, clinicalNotes, "a"))
        (ruleAdded.assignment!!.expression as CommentTemplate).text shouldBe "B"
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
            child {
                +"B"
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()

        val action = ChangeTreeToReplaceAssignment(commentFactory.comment("A"), commentFactory.comment("B"))
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