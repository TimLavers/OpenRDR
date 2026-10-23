package io.rippledown.kb

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.ChangeLogStore
import io.rippledown.persistence.inmemory.InMemoryChangeLogStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChangeLogTest {
    private lateinit var changeLog: ChangeLog
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private var now = 1_000L

    @BeforeTest
    fun setup() {
        changeLog = ChangeLog(InMemoryChangeLogStore()) { now }
    }

    @Test
    fun `initially empty`() {
        changeLog.all().shouldBeEmpty()
    }

    @Test
    fun `recording stamps the record with the user, the time and an id`() {
        // Given
        now = 1_234L

        // When
        val record = changeLog.record(alice, ChangeKind.RULES_ADDED, "Added a rule.", ruleIds = setOf(5))

        // Then
        record.userId shouldBe "alice"
        record.kind shouldBe ChangeKind.RULES_ADDED
        record.summary shouldBe "Added a rule."
        record.ruleIds shouldBe setOf(5)
        record.attributeIds shouldBe emptySet()
        record.timestamp shouldBe 1_234L
        record.id shouldBe 1
        changeLog.all() shouldBe listOf(record)
    }

    @Test
    fun `the default clock is the system clock`() {
        // Given
        val before = System.currentTimeMillis()

        // When
        val record = ChangeLog(InMemoryChangeLogStore()).record(alice, ChangeKind.RULES_ADDED, "Added a rule.")

        // Then
        (record.timestamp >= before) shouldBe true
        (record.timestamp <= System.currentTimeMillis()) shouldBe true
    }

    @Test
    fun `a record the store cannot persist is still returned, without an id`() {
        // Given
        val store = mockk<ChangeLogStore>()
        every { store.create(any()) } throws RuntimeException("database down")
        val log = ChangeLog(store) { 99L }

        // When
        val record = log.record(bob, ChangeKind.DESCRIPTION_SET, "Changed the description.")

        // Then
        record.id.shouldBeNull()
        record shouldBe ChangeRecord(null, "bob", 99L, ChangeKind.DESCRIPTION_SET, "Changed the description.")
    }

    @Test
    fun `attribute ids are kept`() {
        // When
        val record = changeLog.record(alice, ChangeKind.ATTRIBUTE_RENAMED, "Renamed.", attributeIds = setOf(3, 4))

        // Then
        record.attributeIds shouldBe setOf(3, 4)
    }

    @Test
    fun `records are listed oldest first`() {
        // Given
        val r1 = changeLog.record(alice, ChangeKind.RULES_ADDED, "One.")
        val r2 = changeLog.record(bob, ChangeKind.CASE_DELETED, "Two.")
        val r3 = changeLog.record(alice, ChangeKind.RULES_UNDONE, "Three.")

        // Then
        changeLog.all() shouldBe listOf(r1, r2, r3)
    }

    @Test
    fun `the most recent records are the last ones, newest first`() {
        // Given
        changeLog.record(alice, ChangeKind.RULES_ADDED, "One.")
        val r2 = changeLog.record(bob, ChangeKind.CASE_DELETED, "Two.")
        val r3 = changeLog.record(alice, ChangeKind.RULES_UNDONE, "Three.")

        // Then
        changeLog.mostRecent(2) shouldBe listOf(r3, r2)
    }

    @Test
    fun `asking for more recent records than exist returns them all`() {
        // Given
        val r1 = changeLog.record(alice, ChangeKind.RULES_ADDED, "One.")

        // Then
        changeLog.mostRecent(10) shouldBe listOf(r1)
    }

    @Test
    fun `records since an id are those with a greater id, oldest first`() {
        // Given
        val r1 = changeLog.record(alice, ChangeKind.RULES_ADDED, "One.")
        val r2 = changeLog.record(bob, ChangeKind.CASE_DELETED, "Two.")
        val r3 = changeLog.record(alice, ChangeKind.RULES_UNDONE, "Three.")

        // Then
        changeLog.since(requireNotNull(r1.id)) shouldBe listOf(r2, r3)
        changeLog.since(requireNotNull(r3.id)).shouldBeEmpty()
        changeLog.since(0) shouldBe listOf(r1, r2, r3)
    }

    @Test
    fun `records since nothing in particular are all of them`() {
        // Given
        val r1 = changeLog.record(alice, ChangeKind.RULES_ADDED, "One.")
        val r2 = changeLog.record(bob, ChangeKind.CASE_DELETED, "Two.")

        // Then
        changeLog.since(null) shouldBe listOf(r1, r2)
    }

    @Test
    fun `records concerning an attribute are filtered by id`() {
        // Given
        val r1 = changeLog.record(alice, ChangeKind.ATTRIBUTE_RENAMED, "One.", attributeIds = setOf(7))
        changeLog.record(bob, ChangeKind.DEFINITION_EDITED, "Two.", attributeIds = setOf(8))
        val r3 = changeLog.record(alice, ChangeKind.RULES_ADDED, "Three.", attributeIds = setOf(7, 8))

        // Then
        changeLog.concerning(7, 10) shouldBe listOf(r3, r1)
        changeLog.concerning(9, 10).shouldBeEmpty()
    }

    @Test
    fun `records concerning an attribute are limited to the newest`() {
        // Given
        changeLog.record(alice, ChangeKind.ATTRIBUTE_RENAMED, "One.", attributeIds = setOf(7))
        val r2 = changeLog.record(bob, ChangeKind.DEFINITION_EDITED, "Two.", attributeIds = setOf(7))
        val r3 = changeLog.record(alice, ChangeKind.RULES_ADDED, "Three.", attributeIds = setOf(7))

        // Then
        changeLog.concerning(7, 2) shouldBe listOf(r3, r2)
    }
}
