package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class CaseViewExporter(private val destination: File, val attributes: List<Attribute>) {
    init {
        if (destination.exists()) {
            require(destination.isFile) {
                "Case view export destination ${destination.name} is not a file."
            }
            require(destination.length() == 0L) {
                "Case view export file ${destination.name} is not empty."
            }
        }
    }

    fun export() {
        val outputWriter = BufferedWriter(FileWriter(destination))
        attributes.forEach{
            outputWriter.write("${it.id}")
            outputWriter.newLine()
        }
        outputWriter.close()
    }
}