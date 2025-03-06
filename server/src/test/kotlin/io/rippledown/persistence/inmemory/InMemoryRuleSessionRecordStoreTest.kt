package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryRuleSessionRecordStoreTest {
    private lateinit var store: RuleSessionRecordStore
    private var index = 0

    @BeforeTest
    fun setup() {
        store = InMemoryRuleSessionRecordStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptyList()
        index = 0
    }

    @Test
    fun `store histories`() {
        val r1 = store.store(rsr(1, 2, 3))
        store.all() shouldBe listOf(r1)
        val r2 = store.store(rsr(11, 12, 13))
        store.all() shouldBe listOf(r1, r2)
        val r3 = store.store(rsr(110, 120, 130))
        val r4 = store.store(rsr(1100, 1200, 1300))
        val r5 = store.store(rsr(7, 8, 9))
        store.all() shouldBe listOf(r1, r2, r3, r4, r5)
    }

    @Test
    fun `storing a history creates and returns a new history with maximal index`() {
        val r1 = rsr(1, 2, 3)
        val stored1 = store.store(r1)

        val r2 = rsr(4, 5)
        val stored2 = store.store(r2)
        stored2.index shouldBeGreaterThan stored1.index

        val r3 = rsr(6)
        val stored3 = store.store(r3)
        stored3.index shouldBeGreaterThan stored2.index

        val r4 = rsr(8, 9)
        val stored4 = store.store(r4)
        stored4.index shouldBeGreaterThan stored3.index
    }

    @Test
    fun `cannot store a history that shares an id with one already stored`() {
        store.store(rsr(1, 3, 5))
        store.store(rsr(2, 4, 6))
        store.store(rsr(7, 9))
        shouldThrow<IllegalArgumentException> {
            store.store(rsr(10, 3, 50))
        }.message shouldBe "New record shares ids with other rules: [3]"
    }

    @Test
    fun `delete last added has no effect if empty`() {
        store.deleteLastAdded()
        store.all() shouldBe emptyList()
    }

    @Test
    fun `delete last added`() {
        val record1 = store.store(rsr(1))
        val record2 = store.store(rsr(2))
        val record3 = store.store(rsr(3))
        val record4 = store.store(rsr(4, 5))
        store.all() shouldBe listOf(record1, record2, record3, record4)
        store.deleteLastAdded()
        store.all() shouldBe listOf(record1, record2, record3)
        store.deleteLastAdded()
        store.all() shouldBe listOf(record1, record2)
        store.deleteLastAdded()
        store.all() shouldBe listOf(record1)
        store.deleteLastAdded()
        store.all() shouldBe emptyList()
    }

    @Test
    fun `last added`() {
        store.lastAdded() shouldBe null
        val r1 = store.store(rsr(1))
        store.lastAdded() shouldBe r1
        val r2 = store.store(rsr(2))
        store.lastAdded() shouldBe r2
        store.store(rsr(3))
        val r4 = store.store(rsr(4, 5))
        store.lastAdded() shouldBe r4
    }

    @Test
    fun `cannot load items if the store is not empty`() {
        store.store(rsr(1, 3))
        shouldThrow<IllegalArgumentException> {
            store.load(listOf())
        }.message shouldBe "Load should not be called if there are already items stored."
    }

    @Test
    fun `load items`() {
        val r1 = rsr(1, 3)
        val r2 = rsr(2, 4)
        val r3 = rsr(5, 6)
        store.load(listOf(r1, r2, r3))
        store.all() shouldBe listOf(r1, r2, r3)
    }

    @Test
    fun `apply delete to items that have been added by load`() {
        val r1 = rsr(1, 3)
        val r2 = rsr(2, 4)
        val r3 = rsr(5, 6)
        store.load(listOf(r1, r2, r3))
        store.all() shouldBe listOf(r1, r2, r3)
        store.deleteLastAdded()
        store.all() shouldBe listOf(r1, r2)
        store.deleteLastAdded()
        store.all() shouldBe listOf(r1)
    }
    
    private fun rsr(vararg ruleIds: Int)  = RuleSessionRecord(index++, ruleIds.toSet())
}