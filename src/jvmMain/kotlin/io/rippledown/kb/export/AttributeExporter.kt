package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class AttributeExporter(private val destination: File, val attributes: List<Attribute>) {
    init {
        if (destination.exists()) {
            require(destination.isFile) {
                "Attribute export destination ${destination.name} is not a file."
            }
            require(destination.length() == 0L) {
                "Attribute export file ${destination.name} is not empty."
            }
        }
    }

    fun export() {
        val outputWriter = BufferedWriter(FileWriter(destination))
        attributes.forEach{
            outputWriter.write(it.name)
            outputWriter.newLine()
        }
        outputWriter.close()
    }
}