package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class CaseViewImporter(private val source: Path, private val idToAttribute: Map<Int, Attribute>) {

    fun import(): List<Attribute> {
        return Files.readAllLines(source).map { idToAttribute[it.toInt()]!! }
    }
}