package io.rippledown.kb.export

import io.rippledown.model.Attribute
import java.io.File

class CaseViewImporter(private val source: File, private val idToAttribute: Map<Int, Attribute>) {

    fun import(): List<Attribute> {
        return source.readLines().map { idToAttribute[it.toInt()]!! }
    }
}