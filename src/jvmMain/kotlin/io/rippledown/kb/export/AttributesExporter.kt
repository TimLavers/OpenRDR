package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class AttributesExporter(private val destination: File, val attributes: Set<Attribute>) {
    init {
        if (destination.exists()) {
            require(destination.isFile) {
                "Attributes export destination ${destination.name} is not a file."
            }
            require(destination.length() == 0L) {
                "Attributes export file ${destination.name} is not empty."
            }
        }
    }

    fun export() {
        val outputWriter = BufferedWriter(FileWriter(destination))
        attributes.forEach{
            outputWriter.write("${it.id} ${it.name}")
            outputWriter.newLine()
        }
        outputWriter.close()
    }
}