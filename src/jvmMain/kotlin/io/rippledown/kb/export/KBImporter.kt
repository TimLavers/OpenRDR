package io.rippledown.kb.export

import io.rippledown.kb.KB
import io.rippledown.model.Attribute
import java.io.File

class KBImporter(private val source: File) {

    fun import(): KB {
        return KB("blah")
    }
}