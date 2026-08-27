package io.rippledown.model.rule

import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.CommentFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.RuleFactory
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

open class RuleTreeChangeTest : RuleTestBase() {
    lateinit var tree: RuleTree
    lateinit var ruleFactory: RuleFactory
    lateinit var commentFactory: CommentFactory
    lateinit var conditionFactory: DummyConditionFactory
    val A = "A"
    val B = "B"
    lateinit var newAssignment: AssignValue

    open fun setup() {
        commentFactory = CommentFactory()
        conditionFactory = DummyConditionFactory()
        ruleFactory = DummyRuleFactory()
        newAssignment = commentFactory.comment("It is very windy!")
        tree = ruleTree(commentFactory) {
            child {
                +A
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition(conditionFactory) {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
    }
}

internal class ChangeTreeToAddAssignmentTest : RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val changer = ChangeTreeToAddAssignment(newAssignment).createChanger(tree, ruleFactory)
        changer should beInstanceOf<AddAssignmentRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toString = ChangeTreeToAddAssignment(newAssignment).toString()
        toString shouldContain "It is very windy!"
        toString shouldContain newAssignment.attribute.id.toString()
        toString shouldContain ChangeTreeToAddAssignment::class.simpleName.toString()
    }
}

internal class ChangeTreeToRemoveAssignmentTest : RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val changer = ChangeTreeToRemoveAssignment(commentFactory.comment(A)).createChanger(tree, ruleFactory)
        changer should beInstanceOf<RemoveAssignmentRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toGo = commentFactory.comment(A)
        val toString = ChangeTreeToRemoveAssignment(toGo).toString()
        toString shouldContain A
        toString shouldContain toGo.attribute.id.toString()
        toString shouldContain ChangeTreeToRemoveAssignment::class.simpleName.toString()
    }
}

internal class ChangeTreeToReplaceAssignmentTest : RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val toGo = commentFactory.comment(A)
        val changer = ChangeTreeToReplaceAssignment(toGo, newAssignment).createChanger(tree, ruleFactory)
        changer should beInstanceOf<ReplaceAssignmentRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toGo = commentFactory.comment(A)
        val toString = ChangeTreeToReplaceAssignment(toGo, newAssignment).toString()
        toString shouldContain A
        toString shouldContain toGo.attribute.id.toString()
        toString shouldContain "It is very windy!"
        toString shouldContain newAssignment.attribute.id.toString()
        toString shouldContain ChangeTreeToReplaceAssignment::class.simpleName.toString()
    }
}