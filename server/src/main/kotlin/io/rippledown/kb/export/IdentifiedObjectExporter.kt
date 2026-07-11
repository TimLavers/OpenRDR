package io.rippledown.kb.export

import java.nio.file.Files
import java.nio.file.Path

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
class IdentifiedObjectExporter<T>(private val destination: Path, private val objectSource: IdentifiedObjectSource<T>) {
    init {
        checkDirectoryIsSuitableForExport(destination, objectSource.exportType())
    }

    fun export() {
        val items = objectSource.all()
        val ids = items.map { objectSource.idFor(it).toString() }.toSet()
        val idToFilename = FilenameMaker(ids).makeUniqueNames()

        items.forEach{
            val filename = idToFilename[objectSource.idFor(it).toString()]!!
            val destinationFile = destination.resolve(filename)
            val serialized = objectSource.exporter().exportToString(it)
            Files.writeString(destinationFile, serialized)
        }
    }
}
