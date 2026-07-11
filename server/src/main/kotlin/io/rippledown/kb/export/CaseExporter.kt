package io.rippledown.kb.export

import io.rippledown.model.RDRCase
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class CaseExporter(private val destination: Path, val cases: List<RDRCase>) {
    init {
        checkDirectoryIsSuitableForExport(destination, "Case")
    }

    fun export() {
        val caseNames = cases.map { it.name }.toSet()
        val caseNameToFilename = FilenameMaker(caseNames).makeUniqueNames()
        val format = Json { allowStructuredMapKeys = true }

        cases.forEach {
            val serialized = format.encodeToString(RDRCase.serializer(), it)
            val filename = caseNameToFilename[it.name]!!
            val file = destination.resolve(filename)
            Files.writeString(file, serialized)
        }
    }
}