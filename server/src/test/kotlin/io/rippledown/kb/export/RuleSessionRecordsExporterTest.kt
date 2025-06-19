package io.rippledown.kb.export

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore
import io.rippledown.persistence.inmemory.InMemoryRuleSessionRecordStore
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class RuleSessionRecordsExporterTest : ExporterTestBase() {
    @Test
    fun exportToString() {
        val record = RuleSessionRecord(234, 55, setOf(100, 200, 300))
        val exported = RuleSessionRecordsExporter().exportToString(record)
        val rebuilt = RuleSessionRecordsExporter().importFromString(exported)
        rebuilt shouldBe record
    }
}
class RuleSessionRecordsSourceTest : ExporterTestBase() {
    private lateinit var store: RuleSessionRecordStore

    @BeforeEach
    fun setup() {
        store = InMemoryRuleSessionRecordStore()
    }

    @Test
    fun `when empty`() {
        RuleSessionRecordsSource(store.all()).all().shouldBeEmpty()
    }

    @Test
    fun `with data`() {
        val record1 = store.create(RuleSessionRecord(null, 2, setOf(10)))
        val record2 = store.create(RuleSessionRecord(null, 4, setOf(100, 200)))
        val record3 = store.create(RuleSessionRecord(null, 6, setOf(1000, 2000, 3000)))
        with(RuleSessionRecordsSource(store.all()).all()) {
            size shouldBe 3
            this shouldContain record1
            this shouldContain record2
            this shouldContain record3
        }
    }

    @Test
    fun idFor() {
        val record = store.create(RuleSessionRecord(null, 2, setOf(10)))
        RuleSessionRecordsSource(store.all()).idFor(record) shouldBe record.id
    }

    @Test
    fun exporter() {
        val record = store.create(RuleSessionRecord(null, 6, setOf(1000, 2000, 3000)))
        val exported = RuleSessionRecordsSource(store.all()).exporter().exportToString(record)
        val imported = RuleSessionRecordsSource(store.all()).exporter().importFromString(exported)
        imported shouldBe record
    }

    @Test
    fun exportType() {
        RuleSessionRecordsSource(store.all()).exportType() shouldBe "RuleSessionRecords"
    }
}