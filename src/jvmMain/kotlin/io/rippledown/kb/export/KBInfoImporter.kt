package io.rippledown.kb.export

import io.rippledown.model.KBInfo
import java.io.File
import java.nio.file.Files

class KBInfoImporter(private val kbDetailsFile: File) {

    fun import(): KBInfo {
        val lines = Files.readAllLines(kbDetailsFile.toPath())
        val id = lines[0].trim()
        val name = lines[1].trim()
        return KBInfo(id, name)
    }
}