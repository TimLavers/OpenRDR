package io.rippledown.persistence

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.rule.Rule
import kotlin.test.Test

class PersistentRuleTest {

    @Test
    fun `secondary constructor`() {
        val pr = PersistentRule(1, 0, 10, "100,101")
        pr.id shouldBe 1
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe 10
        pr.conditionIds shouldBe setOf(100, 101)
    }

    @Test
    fun `null id`() {
        val pr = PersistentRule(null, 0, 10, "100,101")
        pr.id shouldBe null
    }

    @Test
    fun `root rule constructor`() {
        val pr = PersistentRule()
        pr.id shouldBe null
        pr.parentId shouldBe null
        pr.conditionIds shouldBe emptySet()
    }

    @Test
    fun `no conditions`() {
        val pr = PersistentRule(null, 0, 10, "")
        pr.id shouldBe null
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe 10
        pr.conditionIds shouldBe emptySet()
    }

    @Test
    fun `no conclusion`() {
        val pr = PersistentRule(null, 0, null, "")
        pr.id shouldBe null
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe null
        pr.conditionIds shouldBe emptySet()
    }

    @Test
    fun primaryConstructor() {
        val conditionIdsSet = setOf(100, 101, 102)
        val pr = PersistentRule(null, 0, 10, conditionIdsSet)
        pr.id shouldBe null
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe 10
        pr.conditionIds shouldBe conditionIdsSet
    }

    @Test
    fun conditionIdsString() {
        val conditionIdsSet = setOf(100, 101, 102)
        val pr = PersistentRule(null, 0, 10, conditionIdsSet)
        // This order is necessary, as explained in the code.
        pr.conditionIdsString() shouldBe "100,101,102"
    }

    @Test
    fun `constructor from rule`() {
        val parent = Rule(12, null, null, emptySet())
        val parentPR = PersistentRule(parent)
        parentPR.id shouldBe 12
        parentPR.parentId shouldBe null
        parentPR.conclusionId shouldBe null
        parentPR.conditionIds shouldBe emptySet()
        parentPR.conditionIdsString() shouldBe ""
        val glucose = Attribute("Glucose", 233)
        val condition = IsNormal(33, glucose)
        val child = Rule(13, parent, Conclusion(99, "Blah"), setOf(condition))
        val childPR = PersistentRule(child)
        childPR.id shouldBe 13
        childPR.parentId shouldBe 12
        childPR.conclusionId shouldBe 99
        childPR.conditionIds shouldBe setOf(33)
    }

    @Test
    fun serialization() {
        TODO()
    }
}