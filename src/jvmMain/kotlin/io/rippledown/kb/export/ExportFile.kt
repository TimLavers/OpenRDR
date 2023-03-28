package io.rippledown.kb.export

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class ExportFile(val destination: File, val type: String) {
    init {
        if (destination.exists()) {
            require(destination.isFile) {
                "$type export destination ${destination.name} is not a file."
            }
            require(destination.length() == 0L) {
                "$type export file ${destination.name} is not empty."
            }
        }
    }

    fun writer() = BufferedWriter(FileWriter(destination))
}