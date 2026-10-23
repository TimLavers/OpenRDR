package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.ChangeLogStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresChangeLogStoreTest : PostgresStoreTest() {
    private lateinit var store: ChangeLogStore

    @BeforeTest
    fun setup() {
        clearTables()
        store = postgresKB.changeLogStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.changeLogStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptyList()
    }

    @Test
    fun `created records are returned oldest first with ids assigned`() {
        // When
        val r1 = store.create(record("alice", ChangeKind.RULES_ADDED, ruleIds = setOf(1, 2)))
        val r2 = store.create(record("bob", ChangeKind.DESCRIPTION_SET))
        val r3 = store.create(record("alice", ChangeKind.CASE_DELETED))

        // Then
        store.all() shouldBe listOf(r1, r2, r3)
        r2.id!! shouldBeGreaterThan r1.id!!
        r3.id!! shouldBeGreaterThan r2.id!!

        // And after a reload
        reload()
        store.all() shouldBe listOf(r1, r2, r3)
    }

    @Test
    fun `a created record keeps everything but its id`() {
        // Given
        val record = record("alice", ChangeKind.ATTRIBUTE_RENAMED, attributeIds = setOf(9, 10))
            .copy(summary = "Renamed \"Glucose\" to \"Blood glucose\".", timestamp = 1_700_000_000_123L)

        // When
        val stored = store.create(record)

        // Then
        stored shouldBe record.copy(id = stored.id)
        store.all().single() shouldBe stored
    }

    @Test
    fun `empty id sets round-trip`() {
        // When
        store.create(record("alice", ChangeKind.DESCRIPTION_SET))

        // Then
        reload()
        store.all().single().ruleIds shouldBe emptySet()
        store.all().single().attributeIds shouldBe emptySet()
    }

    @Test
    fun `cannot load items if the store is not empty`() {
        store.create(record("alice", ChangeKind.RULES_ADDED))
        shouldThrow<IllegalArgumentException> {
            store.load(setOf())
        }.message shouldBe "Load should not be called if there are already items stored."
    }

    @Test
    fun `loaded records are returned in id order`() {
        // Given
        val r7 = record("alice", ChangeKind.RULES_ADDED).copy(id = 7, timestamp = 70)
        val r3 = record("bob", ChangeKind.RULES_UNDONE).copy(id = 3, timestamp = 30)

        // When
        store.load(setOf(r7, r3))

        // Then
        store.all() shouldBe listOf(r3, r7)
    }

    @Test
    fun `a record created after a load gets an id above the loaded ones`() {
        // Given
        val r3 = record("alice", ChangeKind.RULES_ADDED).copy(id = 3)
        val r7 = record("bob", ChangeKind.RULES_UNDONE).copy(id = 7)
        store.load(setOf(r3, r7))

        // When
        val created = store.create(record("carol", ChangeKind.CASE_DELETED))

        // Then
        created.id!! shouldBeGreaterThan 7
        store.all() shouldBe listOf(r3, r7, created)
    }

    private fun record(
        userId: String,
        kind: ChangeKind,
        ruleIds: Set<Int> = emptySet(),
        attributeIds: Set<Int> = emptySet()
    ) = ChangeRecord(null, userId, 1_000L, kind, "Did $kind.", ruleIds, attributeIds)
}
