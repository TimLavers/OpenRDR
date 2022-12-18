package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.File

class AttributeImporter(private val source: File) {

    fun import(): List<Attribute> {
        return source.readLines().map { Attribute(it) }
    }
}