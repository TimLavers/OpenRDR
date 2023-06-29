package io.rippledown.kb.export

import io.rippledown.model.RDRCase
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.text.Charsets.UTF_8

class CaseExporter(private val destination: File, val cases: List<RDRCase>) {
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
            val file = File(destination, filename)
            FileUtils.writeStringToFile(file, serialized, UTF_8)
        }
    }
}