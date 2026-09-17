package io.rippledown.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.KbFileDialogRequest
import kotlinx.coroutines.CancellationException

class KbFileTransferController(
    private val dialogs: KbFileDialogs,
    private val api: Api,
    private val onImported: (KBInfo) -> Unit,
    private val onMessage: (String) -> Unit
) {
    enum class State { Idle, ChoosingFile, Transferring }

    var state by mutableStateOf(State.Idle)
        private set
    private val consumedRequestIds = mutableSetOf<String>()

    suspend fun handle(request: KbFileDialogRequest) {
        if (state != State.Idle || !consumedRequestIds.add(request.requestId)) return
        state = State.ChoosingFile
        val operation = if (request is KbFileDialogRequest.Import) "Import" else "Export"
        try {
            val selection = when (request) {
                is KbFileDialogRequest.Import -> dialogs.chooseImportArchive()
                is KbFileDialogRequest.Export -> dialogs.chooseExportDestination(request.kbInfo)
            }
            when (selection) {
                FileSelection.Cancelled -> onMessage("$operation cancelled.")
                is FileSelection.Selected -> {
                    state = State.Transferring
                    when (request) {
                        is KbFileDialogRequest.Import -> {
                            val imported = api.importKBFromZip(selection.file)
                            onImported(imported)
                            onMessage("Imported \"${imported.name}\" and opened it.")
                        }

                        is KbFileDialogRequest.Export -> {
                            api.exportKBToZip(selection.file, request.kbInfo)
                            onMessage("Exported \"${request.kbInfo.name}\" to ${selection.file.absolutePath}.")
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            onMessage("$operation failed: ${failure.message ?: failure.javaClass.simpleName}")
        } finally {
            state = State.Idle
        }
    }
}
