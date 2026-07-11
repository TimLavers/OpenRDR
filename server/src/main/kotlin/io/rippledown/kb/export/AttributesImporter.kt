package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class AttributesImporter(private val source: Path) {

    fun import(): Map<Int, Attribute> {
        val result = mutableMapOf<Int, Attribute>()
        Files.readAllLines(source).forEach{
            val parts = it.split(' ')
            val id = parts[0].toInt()
            result[id] = Attribute(id, parts[1])
        }
        return result
    }
}