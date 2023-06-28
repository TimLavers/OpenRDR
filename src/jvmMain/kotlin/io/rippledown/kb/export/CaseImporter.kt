package io.rippledown.kb.export

import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.text.Charsets.UTF_8

class CaseImporter(private val source: File) {
    init {
        checkIsDirectory(source)
    }

    fun import(): List<RDRCase> {
        val result = mutableListOf<RDRCase>()
        val format = Json { allowStructuredMapKeys = true }
        source.listFiles()?.forEach {
            val data = FileUtils.readFileToString(it, UTF_8)
            result.add(format.decodeFromString(data))
        }
        return result
    }
}