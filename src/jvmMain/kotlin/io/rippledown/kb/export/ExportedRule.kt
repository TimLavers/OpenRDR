package io.rippledown.kb.export

import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.Rule
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

@Serializable
data class ExportedRule(
    val id: String,
    val parentId: String? = null,
    val conclusion: Conclusion? = null,
    val conditions: Set<Condition> = mutableSetOf()
) {

    constructor(rule: Rule) : this(rule.id, rule.parent?.id, rule.conclusion, rule.conditions)

    fun export(destinationFile: File) {
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(this)
        FileUtils.writeStringToFile(destinationFile, serialized, Charsets.UTF_8)
    }
}