package io.rippledown.kb.export

import io.rippledown.model.change.ChangeRecord
import kotlinx.serialization.json.Json

class ChangeRecordsExporter : Exporter<ChangeRecord>, Importer<ChangeRecord> {
    private val json = Json { allowStructuredMapKeys = true }

    override fun exportToString(t: ChangeRecord) = json.encodeToString(t)

    override fun importFromString(data: String) = json.decodeFromString<ChangeRecord>(data)
}

class ChangeRecordsSource(val all: List<ChangeRecord>) : IdentifiedObjectSource<ChangeRecord> {
    override fun all() = all.toSet()

    override fun idFor(t: ChangeRecord) = requireNotNull(t.id) { "A stored change record has an id." }

    override fun exporter() = ChangeRecordsExporter()

    override fun exportType() = "ChangeRecords"
}
