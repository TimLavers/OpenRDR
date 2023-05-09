package io.rippledown.kb.export

import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

class RuleImporter(private val source: File) {
    init {
        checkIsDirectory(source)
        require(source.listFiles()!!.isNotEmpty()) {
            "$source is empty."
        }
    }

    fun import(): Set<PersistentRule> {
        val format = Json { allowStructuredMapKeys = true }
        val result = mutableSetOf<PersistentRule>()
        source.listFiles()?.forEach {
            val data = FileUtils.readFileToString(it, Charsets.UTF_8)
            val persistentRule = format.decodeFromString<PersistentRule>(data)
            result.add(persistentRule)
        }
        return result
    }
}