package io.rippledown.kb.export

import org.apache.commons.io.FileUtils
import java.io.File

interface Exporter<T> {
    fun exportToString(t: T): String
}

interface IdentifiedObjectSource<T> {
    fun all(): Set<T>
    fun idFor(t:T): Int
    fun exporter(): Exporter<T>
    fun exportType(): String
    fun exportFileSuffix(): String = ".json"
}
class IdentifiedObjectExporter<T>(private val destination: File, private val objectSource: IdentifiedObjectSource<T>) {
    init {
        checkDirectoryIsSuitableForExport(destination, objectSource.exportType())
    }

    fun export() {
        val items = objectSource.all()
        val ids = items.map { objectSource.idFor(it).toString() }.toSet()
        val idToFilename = FilenameMaker(ids).makeUniqueNames()

        items.forEach{
            val filename = idToFilename[objectSource.idFor(it).toString()]!!
            val destinationFile = File(destination, filename)
            val serialized = objectSource.exporter().exportToString(it)
            FileUtils.writeStringToFile(destinationFile, serialized, Charsets.UTF_8)
        }
    }
}
