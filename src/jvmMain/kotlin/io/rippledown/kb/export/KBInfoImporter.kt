package io.rippledown.kb.export

import io.rippledown.model.KBInfo
import java.io.File
import java.nio.file.Files

class KBInfoImporter(private val kbDetailsFile: File) {

    fun import(): KBInfo {
        val lines = Files.readAllLines(kbDetailsFile.toPath())
        val name = lines[1].trim()
        // The id is used to identify the db, so we need a new id.
        return KBInfo(name)
    }
}