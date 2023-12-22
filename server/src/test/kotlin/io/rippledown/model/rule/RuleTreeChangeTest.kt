package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.Conclusion
import io.rippledown.model.DummyConclusionFactory
import io.rippledown.model.DummyConditionFactory
import io.rippledown.model.RuleFactory
import io.rippledown.model.rule.dsl.ruleTree
import kotlin.test.BeforeTest
import kotlin.test.Test

open class RuleTreeChangeTest : RuleTestBase() {
    lateinit var tree: RuleTree
    lateinit var ruleFactory: RuleFactory
    lateinit var conclusionFactory: DummyConclusionFactory
    lateinit var conditionFactory: DummyConditionFactory
    val A = "A"
    val B = "B"
    lateinit var newConclusion: Conclusion

    open fun setup() {
        conclusionFactory = DummyConclusionFactory()
        conditionFactory = DummyConditionFactory()
        ruleFactory = DummyRuleFactory()
        newConclusion = conclusionFactory.getOrCreate("It is very windy!")
        tree = ruleTree(conclusionFactory) {
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
internal class ChangeTreeToAddConclusionTest: RuleTreeChangeTest() {
    @BeforeTest
    override fun setup() = super.setup()

    @Test
    fun alignWith() {
        val copyOfNewConclusion = Conclusion(newConclusion.id, newConclusion.text)
        val change = ChangeTreeToAddConclusion(copyOfNewConclusion)
        val aligned = change.alignWith(conclusionFactory)
        aligned.toBeAdded shouldBeSameInstanceAs newConclusion
    }

    @Test
    fun `align with for conclusion that does not match that in the factory`() {
        val copyOfNewConclusion = Conclusion(newConclusion.id * 100, newConclusion.text)
        val change = ChangeTreeToAddConclusion(copyOfNewConclusion)
        shouldThrow<IllegalArgumentException> {
            change.alignWith(conclusionFactory)
        }.message shouldContain "do not match"
    }

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
    fun alignWith() {
        val copyOfNewConclusion = Conclusion(newConclusion.id, newConclusion.text)
        val change = ChangeTreeToRemoveConclusion(copyOfNewConclusion)
        val aligned = change.alignWith(conclusionFactory)
        aligned.toBeRemoved shouldBeSameInstanceAs newConclusion
    }

    @Test
    fun `align with for conclusion that does not match that in the factory`() {
        val copyOfNewConclusion = Conclusion(newConclusion.id * 100, newConclusion.text)
        val change = ChangeTreeToRemoveConclusion(copyOfNewConclusion)
        shouldThrow<IllegalArgumentException> {
            change.alignWith(conclusionFactory)
        }.message shouldContain "do not match"
    }

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
    private lateinit var rainConclusion: Conclusion

    @BeforeTest
    override fun setup() {
        super.setup()
        rainConclusion = conclusionFactory.getOrCreate("It will rain.")
    }

    @Test
    fun alignWith() {
        val copyOfNewConclusion = Conclusion(newConclusion.id, newConclusion.text)
        val copyOfToGo = Conclusion(rainConclusion.id, rainConclusion.text)
        val change = ChangeTreeToReplaceConclusion(copyOfToGo,copyOfNewConclusion)
        val aligned = change.alignWith(conclusionFactory)
        aligned.replacement shouldBeSameInstanceAs newConclusion
        aligned.toBeReplaced shouldBeSameInstanceAs rainConclusion
    }

    @Test
    fun `align with for added conclusion that does not match that in the factory`() {
        val copyOfNewConclusion = Conclusion(newConclusion.id * 100, newConclusion.text)
        val change = ChangeTreeToReplaceConclusion(rainConclusion, copyOfNewConclusion)
        shouldThrow<IllegalArgumentException> {
            change.alignWith(conclusionFactory)
        }.message shouldContain "do not match"
    }

    @Test
    fun `align with for removed conclusion that does not match that in the factory`() {
        val copyOfNewConclusion = Conclusion(newConclusion.id * 100, newConclusion.text)
        val change = ChangeTreeToReplaceConclusion(copyOfNewConclusion, rainConclusion)
        shouldThrow<IllegalArgumentException> {
            change.alignWith(conclusionFactory)
        }.message shouldContain "do not match"
    }

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