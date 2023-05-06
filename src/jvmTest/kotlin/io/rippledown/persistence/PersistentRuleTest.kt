package io.rippledown.persistence

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PersistentRuleTest {

    @Test
    fun primaryConstructor() {
        val pr = PersistentRule(1, 0, 10, "100,101")
        pr.id shouldBe 1
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe 10
        pr.conditionIds shouldBe setOf(100, 101)
    }

    @Test
    fun primaryConstructorNullId() {
        val pr = PersistentRule(null, 0, 10, "100,101")
        pr.id shouldBe null
    }

    @Test
    fun primaryConstructorNoConditions() {
        val pr = PersistentRule(null, 0, 10, "")
        pr.id shouldBe null
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe 10
        pr.conditionIds shouldBe emptySet()
    }

    @Test
    fun primaryConstructorNoConclusion() {
        val pr = PersistentRule(null, 0, null, "")
        pr.id shouldBe null
        pr.parentId shouldBe 0
        pr.conclusionId shouldBe null
        pr.conditionIds shouldBe emptySet()
    }

    @Test
    fun secondaryConstructor() {
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
}