package io.rippledown.model.change

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class ChangeRecordTest {

    @Test
    fun `a record with rule and attribute ids survives serialisation`() {
        // Given
        val record = ChangeRecord(
            id = 7,
            userId = "alice",
            timestamp = 1_700_000_000_000L,
            kind = ChangeKind.RULES_ADDED,
            summary = "Added comment \"Go to the beach.\" when Sun is hot",
            ruleIds = setOf(3, 4),
            attributeIds = setOf(12)
        )

        // When
        val rebuilt = serializeDeserialize(record)

        // Then
        rebuilt shouldBe record
    }

    @Test
    fun `a record with no ids survives serialisation`() {
        // Given
        val record = ChangeRecord(null, "bob", 1L, ChangeKind.DESCRIPTION_SET, "Set the description.")

        // When
        val rebuilt = serializeDeserialize(record)

        // Then
        rebuilt shouldBe record
        rebuilt.ruleIds shouldBe emptySet()
        rebuilt.attributeIds shouldBe emptySet()
    }

    @Test
    fun `a change carries the id of the knowledge base it was made in`() {
        // Given
        val record = ChangeRecord(2, "carol", 5L, ChangeKind.CASE_DELETED, "Deleted case \"Case 1\".")
        val change = KbChange("kb-123", record)

        // When
        val rebuilt = serializeDeserialize(change)

        // Then
        rebuilt shouldBe change
    }

    @Test
    fun `every kind survives serialisation`() {
        ChangeKind.entries.forEach { kind ->
            // Given
            val record = ChangeRecord(1, "alice", 1L, kind, "x")

            // When
            val rebuilt = serializeDeserialize(record)

            // Then
            rebuilt.kind shouldBe kind
        }
    }
}
