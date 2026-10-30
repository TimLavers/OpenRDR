package io.rippledown.kb.export

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.inmemory.InMemoryChangeLogStore
import kotlin.test.Test

class ChangeRecordsExporterTest : ExporterTestBase() {
    private val record = ChangeRecord(
        234, "alice", 1_700_000_000_000L, ChangeKind.RULES_ADDED, "Added comment \"Go.\" when Glucose is high.",
        setOf(100, 200), setOf(7)
    )

    @Test
    fun `export then import`() {
        // When
        val exported = ChangeRecordsExporter().exportToString(record)
        val rebuilt = ChangeRecordsExporter().importFromString(exported)

        // Then
        rebuilt shouldBe record
    }

    @Test
    fun `source when empty`() {
        ChangeRecordsSource(emptyList()).all().shouldBeEmpty()
    }

    @Test
    fun `source with data`() {
        // Given
        val store = InMemoryChangeLogStore()
        val r1 = store.create(record)
        val r2 = store.create(record.copy(userId = "bob"))

        // When
        val source = ChangeRecordsSource(store.all())

        // Then
        source.all() shouldBe setOf(r1, r2)
        source.idFor(r1) shouldBe r1.id
        source.exportType() shouldBe "ChangeRecords"
        source.exporter().importFromString(source.exporter().exportToString(r2)) shouldBe r2
    }
}
