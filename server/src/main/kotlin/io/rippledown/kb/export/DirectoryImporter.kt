package io.rippledown.kb.export

import org.apache.commons.io.FileUtils
import java.io.File

interface Importer<T> {
    fun importFromString(data: String): T
}
class DirectoryImporter<T>(private val source: File, private val importer: Importer<T>, allowEmpty: Boolean = false) {
    init {
        checkIsDirectory(source)
        require(allowEmpty || source.listFiles()!!.isNotEmpty()) {
            "$source is empty."
        }
    }

    fun import(): Set<T> {
        val result = mutableSetOf<T>()
        source.listFiles()?.forEach {
            val data = FileUtils.readFileToString(it, Charsets.UTF_8)
            result.add(importer.importFromString(data))
        }
        return result
    }
}