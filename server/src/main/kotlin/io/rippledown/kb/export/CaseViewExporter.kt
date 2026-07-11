package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class CaseViewExporter(private val destination: Path, val attributes: List<Attribute>) {
    init {
        if (destination.exists()) {
            require(!destination.isDirectory()) {
                "Case view export destination ${destination.name} is not a file."
            }
            require(destination.fileSize() == 0L) {
                "Case view export file ${destination.name} is not empty."
            }
        }
    }

    fun export() {
        Files.newBufferedWriter(destination).use { outputWriter ->
            attributes.forEach {
                outputWriter.write("${it.id}")
                outputWriter.newLine()
            }
        }
    }
}