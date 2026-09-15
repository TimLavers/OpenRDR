package io.rippledown.files

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Window
import java.io.File

interface FileDialogLauncher {
    suspend fun openFile(parent: Window, title: String, extensions: Set<String>): File?
    suspend fun saveFile(parent: Window, title: String, suggestedName: String, extension: String): File?
}

object FileKitDialogLauncher : FileDialogLauncher {
    override suspend fun openFile(parent: Window, title: String, extensions: Set<String>): File? =
        withContext(Dispatchers.IO) {
            FileKit.openFilePicker(
                type = FileKitType.File(extensions),
                dialogSettings = FileKitDialogSettings(title = title, parentWindow = parent)
            )?.file
        }

    override suspend fun saveFile(parent: Window, title: String, suggestedName: String, extension: String): File? =
        withContext(Dispatchers.IO) {
            FileKit.openFileSaver(
                suggestedName = suggestedName,
                defaultExtension = extension,
                allowedExtensions = setOf(extension),
                dialogSettings = FileKitDialogSettings(title = title, parentWindow = parent)
            )?.file
        }
}
