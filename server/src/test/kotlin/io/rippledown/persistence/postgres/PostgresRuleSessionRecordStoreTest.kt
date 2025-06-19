package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresRuleSessionRecordStoreTest: PostgresStoreTest() {
    private lateinit var store: RuleSessionRecordStore

    override fun tablesInDropOrder() = listOf(RULE_SESSIONS_TABLE)

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.ruleSessionRecordStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.ruleSessionRecordStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptyList()
    }

    @Test
    fun `store histories`() {
        val r1 = store.create(rsr(1, 2, 3))
        store.all() shouldBe listOf(r1)
        val r2 = store.create(rsr(11, 12, 13))
        store.all() shouldBe listOf(r1, r2)
        val r3 = store.create(rsr(110, 120, 130))
        val r4 = store.create(rsr(1100, 1200, 1300))
        val r5 = store.create(rsr(7, 8, 9))
        store.all() shouldBe listOf(r1, r2, r3, r4, r5)
    }

    @Test
    fun `storing a history creates and returns a new history with maximal index`() {
        val r1 = rsr(1, 2, 3)
        val stored1 = store.create(r1)

        val r2 = rsr(4, 5)
        val stored2 = store.create(r2)
        stored2.index shouldBeGreaterThan stored1.index

        val r3 = rsr(6)
        val stored3 = store.create(r3)
        stored3.index shouldBeGreaterThan stored2.index

        val r4 = rsr(8, 9)
        val stored4 = store.create(r4)
        stored4.index shouldBeGreaterThan stored3.index
    }

    @Test
    fun deleteImpl() {
        val record1 = store.create(rsr(1))
        val record2 = store.create(rsr(2))
        val record3 = store.create(rsr(3))
        val record4 = store.create(rsr(4, 5))
        store.all() shouldBe listOf(record1, record2, record3, record4)
        store.deleteImpl(record3)
        store.all() shouldBe listOf(record1, record2, record4)
        store.deleteImpl(record1)
        store.all() shouldBe listOf(record2, record4)
        store.deleteImpl(record2)
        store.all() shouldBe listOf(record4)
        store.deleteImpl(record4)
        store.all() shouldBe emptyList()
    }

    @Test
    fun `deleting a record not in the store does nothing`() {
        val record1 = store.create(rsr(1))
        val record2 = store.create(rsr(2))
        val record3 = store.create(rsr(3))
        val record4 = store.create(rsr(4, 5))
        store.all() shouldBe listOf(record1, record2, record3, record4)
        store.deleteImpl(record3)
        store.all() shouldBe listOf(record1, record2, record4)
        store.deleteImpl(record3)
        store.all() shouldBe listOf(record1, record2, record4)
    }

    @Test
    fun `deleting a record that has no id fails`() {
        val record1 = store.create(rsr(1))
        val record2 = store.create(rsr(2))
        store.all() shouldBe listOf(record1, record2)
        shouldThrow<Exception> {
            store.deleteImpl(RuleSessionRecord(null, 3, setOf(8)))
        }
        store.all() shouldBe listOf(record1, record2)
    }

    @Test
    fun `delete last added has no effect if empty`() {
        store.deleteLastAdded()
        store.all() shouldBe emptyList()
    }

    @Test
    fun `delete last added`() {
        val record1 = store.create(rsr(1))
        val record2 = store.create(rsr(2))
        val record3 = store.create(rsr(3))
        val record4 = store.create(rsr(4, 5))
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
        val r1 = store.create(rsr(1))
        store.lastAdded() shouldBe r1
        val r2 = store.create(rsr(2))
        store.lastAdded() shouldBe r2
        store.create(rsr(3))
        val r4 = store.create(rsr(4, 5))
        store.lastAdded() shouldBe r4
    }

    @Test
    fun `cannot load items if the store is not empty`() {
        store.create(rsr(1, 3))
        shouldThrow<IllegalArgumentException> {
            store.load(setOf())
        }.message shouldBe "Load should not be called if there are already items stored."
    }

    @Test
    fun `load items`() {
        val r1 = rsr(1, 3).copy(id = 7)
        val r2 = rsr(2, 4).copy(id = 8)
        val r3 = rsr(5, 6).copy(id = 9)
        store.load(setOf(r1, r2, r3))
        store.all() shouldBe listOf(r1, r2, r3)
    }

    @Test
    fun `apply delete to items that have been added by load`() {
        val r1 = rsr(1, 3).copy(id = 10)
        val r2 = rsr(2, 4).copy(id = 20)
        val r3 = rsr(5, 6).copy(id = 30)
        store.load(setOf(r1, r2, r3))
        store.all() shouldBe listOf(r1, r2, r3)
        store.deleteLastAdded()
        store.all() shouldBe listOf(r1, r2)
        store.deleteLastAdded()
        store.all() shouldBe listOf(r1)
    }

    private fun rsr(vararg ruleIds: Int) = RuleSessionRecord(null, 0, ruleIds.toSet())
}