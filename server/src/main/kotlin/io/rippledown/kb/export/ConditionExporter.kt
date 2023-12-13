package io.rippledown.kb.export

import io.rippledown.kb.ConditionManager
import io.rippledown.model.condition.Condition
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConditionExporter: Exporter<Condition>, Importer<Condition> {
    override fun exportToString(t: Condition) = Json.encodeToString(t)
    override fun importFromString(data: String) = Json.decodeFromString<Condition>(data)
}
class ConditionSource(val conditionManager: ConditionManager): IdentifiedObjectSource<Condition> {
    override fun all() = conditionManager.all()

    override fun idFor(t: Condition) = t.id!!

    override fun exporter() = ConditionExporter()

    override fun exportType() = "Condition"
}