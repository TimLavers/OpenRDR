package io.rippledown.model.rule

import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.Conclusion
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.RuleFactory
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

open class RuleTreeChangeTest : RuleTestBase() {
    lateinit var tree: RuleTree
    lateinit var ruleFactory: RuleFactory
    lateinit var conclusionFactory: DummyConclusionFactory
    val A = "A"
    val B = "B"
    lateinit var newConclusion: Conclusion

    open fun setup() {
        conclusionFactory = DummyConclusionFactory()
        ruleFactory = DummyRuleFactory()
        newConclusion = conclusionFactory.getOrCreate("It is very windy!")
        tree = ruleTree(conclusionFactory) {
            child {
                +A
                condition {
                    attribute = clinicalNotes
                    constant = "a"
                }
            }
            child {
                +B
                condition {
                    attribute = clinicalNotes
                    constant = "b"
                }
            }
        }.build()
    }
}
internal class ChangeTreeToAddConclusionTest: RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val changer = ChangeTreeToAddConclusion(newConclusion).createChanger(tree, ruleFactory)
        changer should  beInstanceOf<AddConclusionRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toString = ChangeTreeToAddConclusion(newConclusion).toString()
        toString shouldContain newConclusion.text
        toString shouldContain newConclusion.id.toString()
        toString shouldContain ChangeTreeToAddConclusion::class.simpleName.toString()
    }
}
internal class ChangeTreeToRemoveConclusionTest: RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val changer = ChangeTreeToRemoveConclusion(findOrCreateConclusion(A, tree.root)).createChanger(tree, ruleFactory)
        changer should  beInstanceOf<RemoveConclusionRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toGo = findOrCreateConclusion(A, tree.root)
        val toString = ChangeTreeToRemoveConclusion(toGo).toString()
        toString shouldContain toGo.text
        toString shouldContain toGo.id.toString()
        toString shouldContain ChangeTreeToRemoveConclusion::class.simpleName.toString()
    }
}
internal class ChangeTreeToReplaceConclusionTest: RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun createChanger() {
        val toGo = findOrCreateConclusion(A, tree.root)
        val changer = ChangeTreeToReplaceConclusion(toGo, newConclusion).createChanger(tree, ruleFactory)
        changer should  beInstanceOf<ReplaceConclusionRuleTreeChanger>()
        changer.ruleFactory shouldBeSameInstanceAs ruleFactory
        changer.ruleTree shouldBeSameInstanceAs tree
    }

    @Test
    fun toStringTest() {
        val toGo = findOrCreateConclusion(A, tree.root)
        val toString = ChangeTreeToReplaceConclusion(toGo, newConclusion).toString()
        toString shouldContain toGo.text
        toString shouldContain toGo.id.toString()
        toString shouldContain newConclusion.text
        toString shouldContain newConclusion.id.toString()
        toString shouldContain ChangeTreeToReplaceConclusion::class.simpleName.toString()
    }
}