package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.File

class AttributesImporter(private val source: File) {

    fun import(): Map<Int, Attribute> {
        val result = mutableMapOf<Int, Attribute>()
        source.readLines().forEach{
            val parts = it.split(' ')
            val id = parts[0].toInt()
            result[id] = Attribute(parts[1], id)
        }
        return result
    }
}