package io.rippledown.persistence

import io.kotest.matchers.shouldBe
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
}