package io.rippledown.kb.export

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

interface Importer<T> {
    fun importFromString(data: String): T
}
class DirectoryImporter<T>(private val source: Path, private val importer: Importer<T>, allowEmpty: Boolean = false) {
    init {
        checkIsDirectory(source)
        require(allowEmpty || source.listDirectoryEntries().isNotEmpty()) {
            "$source is empty."
        }
    }

    fun import(): Set<T> {
        val result = mutableSetOf<T>()
        source.listDirectoryEntries().forEach {
            val data = Files.readString(it)
            result.add(importer.importFromString(data))
        }
        return result
    }
}