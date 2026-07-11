package io.rippledown.kb.export

import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

class CaseImporter(private val source: Path) {
    init {
        checkIsDirectory(source)
    }

    fun import(): List<RDRCase> {
        val result = mutableListOf<RDRCase>()
        val format = Json { allowStructuredMapKeys = true }
        source.listDirectoryEntries().forEach {
            val data = Files.readString(it)
            result.add(format.decodeFromString(data))
        }
        return result
    }
}