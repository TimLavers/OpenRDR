package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

data class ExportedRule(val persistentRule: PersistentRule) {

    constructor(rule: Rule) : this(PersistentRule(rule))

    fun export(destinationFile: File) {
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(persistentRule)
        FileUtils.writeStringToFile(destinationFile, serialized, Charsets.UTF_8)
    }
}