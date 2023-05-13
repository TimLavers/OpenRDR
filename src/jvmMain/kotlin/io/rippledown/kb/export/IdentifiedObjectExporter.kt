package io.rippledown.kb.export

import org.apache.commons.io.FileUtils
import java.io.File

interface Exporter<T> {
    fun serializeAsString(t: T): String
}

interface IdentifiedObjectSource<T> {
    fun all(): Set<T>
    fun idFor(t:T): Int
    fun exporter(): Exporter<T>
    fun exportType(): String
}
class IdentifiedObjectExporter<T>(private val destination: File, private val objectSource: IdentifiedObjectSource<T>) {
    init {
        checkDirectoryIsSuitableForExport(destination, objectSource.exportType())
    }

    fun export() {
        val rules = objectSource.all()
        val ruleIds = rules.map { objectSource.idFor(it).toString() }.toSet()
        val ruleIdToFilename = FilenameMaker(ruleIds).makeUniqueNames()

        rules.forEach{
            val filename = ruleIdToFilename[objectSource.idFor(it).toString()]!!
            val destinationFile = File(destination, filename)
            val serialized = objectSource.exporter().serializeAsString(it)
            FileUtils.writeStringToFile(destinationFile, serialized, Charsets.UTF_8)
        }
    }
}
