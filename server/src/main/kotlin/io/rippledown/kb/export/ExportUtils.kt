package io.rippledown.kb.export

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

fun checkDirectoryIsSuitableForExport(destination: Path, exportType: String) {
    require(destination.exists()) {
        "$exportType export destination is not an existing directory."
    }
    require(destination.isDirectory()) {
        "$exportType export destination is not a directory."
    }
    require(destination.listDirectoryEntries().isEmpty()) {
        "$exportType export directory is not empty."
    }
}

fun checkIsDirectory(path: Path) {
    require(path.exists() && path.isDirectory()) {
        "$path is not an existing directory."
    }
}