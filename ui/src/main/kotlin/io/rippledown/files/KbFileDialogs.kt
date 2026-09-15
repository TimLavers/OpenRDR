package io.rippledown.files

import io.rippledown.model.KBInfo
import java.io.File

sealed interface FileSelection {
    data class Selected(val file: File) : FileSelection
    data object Cancelled : FileSelection
}

interface KbFileDialogs {
    suspend fun chooseImportArchive(): FileSelection
    suspend fun chooseExportDestination(kbInfo: KBInfo): FileSelection
}
