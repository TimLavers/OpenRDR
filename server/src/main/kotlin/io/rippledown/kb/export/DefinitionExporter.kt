package io.rippledown.kb.export

import io.rippledown.kb.DerivedDefinitionManager
import io.rippledown.model.rule.ValueExpression
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The definition of one attribute, as exported. Once comments are comment
 * attributes their text is a definition rather than a conclusion, so an
 * export that omitted the definitions would lose every comment in the KB.
 * See "Phase 2 — comments become derived attributes" in
 * documentation/design/repeat_inferencing.md.
 */
@Serializable
data class ExportedDefinition(val attributeId: Int, val expression: ValueExpression)

class DefinitionExporter : Exporter<ExportedDefinition>, Importer<ExportedDefinition> {
    override fun exportToString(t: ExportedDefinition) = Json.encodeToString(t)
    override fun importFromString(data: String) = Json.decodeFromString<ExportedDefinition>(data)
}

class DefinitionSource(private val definitionManager: DerivedDefinitionManager) :
    IdentifiedObjectSource<ExportedDefinition> {
    override fun all() = definitionManager.all()
        .map { ExportedDefinition(it.key, it.value) }
        .toSet()

    override fun idFor(t: ExportedDefinition) = t.attributeId

    override fun exporter() = DefinitionExporter()

    override fun exportType() = "Definition"
}
