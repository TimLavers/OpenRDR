package io.rippledown.files

import io.rippledown.constants.main.EXPORT_KB_TEXT
import io.rippledown.constants.main.IMPORT_KB_TEXT
import io.rippledown.model.KBInfo
import java.awt.Window

class FileKitKbFileDialogs(
    private val parentWindow: Window,
    private val launcher: FileDialogLauncher = FileKitDialogLauncher
) : KbFileDialogs {
    override suspend fun chooseImportArchive(): FileSelection =
        launcher.openFile(parentWindow, IMPORT_KB_TEXT, setOf("zip"))
            ?.let(FileSelection::Selected) ?: FileSelection.Cancelled

    override suspend fun chooseExportDestination(kbInfo: KBInfo): FileSelection =
        launcher.saveFile(parentWindow, EXPORT_KB_TEXT, suggestedFileName(kbInfo.name), "zip")
            ?.let(FileSelection::Selected) ?: FileSelection.Cancelled

    private fun suggestedFileName(kbName: String): String {
        val name = kbName.map { if (it < ' ' || it in "<>:\"/\\|?*") '_' else it }
            .joinToString("").trim().trimEnd(' ', '.')
            .ifEmpty { "knowledge-base" }
        val reserved = Regex("CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³]", RegexOption.IGNORE_CASE)
        return if (reserved.matches(name.substringBefore('.').trimEnd())) "_$name" else name
    }
}
