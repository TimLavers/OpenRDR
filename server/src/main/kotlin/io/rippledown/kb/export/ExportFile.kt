package io.rippledown.kb.export

import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ExportFile(val destination: Path, val type: String) {
    init {
        if (destination.exists()) {
            require(!destination.isDirectory()) {
                "$type export destination ${destination.name} is not a file."
            }
            require(destination.fileSize() == 0L) {
                "$type export file ${destination.name} is not empty."
            }
        }
    }

    fun writer(): BufferedWriter = Files.newBufferedWriter(destination)
}