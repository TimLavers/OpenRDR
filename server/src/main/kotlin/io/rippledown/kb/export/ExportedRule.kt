package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

data class ExportedRule(val persistentRule: PersistentRule) {

    constructor(rule: Rule) : this(PersistentRule(rule))

    fun export(destinationFile: Path) {
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(persistentRule)
        Files.writeString(destinationFile, serialized)
    }
}