package io.rippledown.kb.export

import java.io.File

fun checkDirectoryIsSuitableForExport(destination: File, exportType: String) {
    require(destination.exists()) {
        "$exportType export destination is not an existing directory."
    }
    require(destination.isDirectory) {
        "$exportType export destination is not a directory."
    }
    destination.listFiles()?.let {
        require(it.isEmpty()) {
            "$exportType export directory is not empty."
        }
    }
}