package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleSessionRecordStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuleSessionRecordsExporter: Exporter<RuleSessionRecord>, Importer<RuleSessionRecord> {
    private val json = Json { allowStructuredMapKeys = true }

    override fun exportToString(t: RuleSessionRecord) = json.encodeToString(t)

    override fun importFromString(data: String) = json.decodeFromString<RuleSessionRecord>(data)
}
class RuleSessionRecordsSource(val all: List<RuleSessionRecord>): IdentifiedObjectSource<RuleSessionRecord> {
    override fun all() = all.toSet()

    override fun idFor(t: RuleSessionRecord) = t.id!!

    override fun exporter() = RuleSessionRecordsExporter()

    override fun exportType() = "RuleSessionRecords"
}