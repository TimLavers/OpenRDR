package io.rippledown.kb.export

import io.rippledown.model.RDRCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.text.Charsets.UTF_8

class CaseExporter(val destination: File, val cases: Set<RDRCase>) {
    init {
        require(destination.exists()) {
            "Case export destination is not an existing directory."
        }
        require(destination.isDirectory()) {
            "Case export destination is not a directory."
        }
        destination.listFiles()?.let {
            require(it.isEmpty()) {
                "Case export destination is not empty."
            }
        }
    }

    fun export() {
        val caseNames = cases.map { it.name }.toSet()
        val caseNameToFilename = FilenameMaker(caseNames).makeUniqueNames()
        val format = Json { allowStructuredMapKeys = true }

        cases.forEach{
            val serialized = format.encodeToString(it)
            val filename = caseNameToFilename[it.name]!!
            val file = File(destination, filename)
            FileUtils.writeStringToFile(file, serialized, UTF_8)
        }
    }
}