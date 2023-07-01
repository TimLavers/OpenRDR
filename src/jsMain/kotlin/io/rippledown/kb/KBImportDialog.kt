package io.rippledown.kb

import Handler
import debug
import io.rippledown.constants.kb.*
import mui.material.*
import react.FC
import react.dom.html.ReactHTML.input
import react.useState
import web.file.File
import web.html.InputType
import web.timers.Timeout
import web.timers.clearInterval
import web.timers.setInterval

external interface KBImportDialogHandler : Handler {
    var reloadKB: () -> Unit
}

val KBImportDialog = FC<KBImportDialogHandler> { handler ->
    var isOpen by useState(false)
    var canSubmit by useState(false)
    var selectedFile: File? by useState()
    var timerId: Timeout? = null

    fun waitForImportToFinish() {
        timerId = setInterval({
            val inProgress = handler.api.importInProgress()
            if (!inProgress) {
                handler.reloadKB()
                clearInterval(timerId!!)
            }
        }, 100)
    }

    Button {
        +"Import"
        id = KB_IMPORT_BUTTON_ID
        variant = ButtonVariant.outlined
        size = Size.small
        onClick = {
            isOpen = true
        }
    }

    Dialog {
        id = KB_IMPORT_DIALOG
        open = isOpen
        debug("Import dialog open: $isOpen")
        DialogTitle {
            +"Import KB from zip file"
        }
        DialogContent {
            id = KB_IMPORT_DIALOG_CONTENT
            DialogContentText {
                +"Select a zip file containing an exported Open RippleDown KB"
            }
            input {
                type = InputType.file
                pattern = "*.zip"
                id = "select_zip"
                name = "select_zip"
                onChange = {
                    if (it.target.files != null && it.target.files!!.length == 1) {
                        val sf = it.currentTarget.files!!.item(0)!!
                        if (sf.name.endsWith("zip")) {
                            selectedFile = sf
                            canSubmit = true
                        }
                    }
                }
            }
        }
        DialogActions {
            Button {
                onClick = { isOpen = false }
                +"Cancel"
                id = CANCEL_IMPORT_BUTTON_ID
            }
            debug("rendering import button")
            Button {
                onClick = {
                    isOpen = false
                    handler.api.importKBFromZip(selectedFile!!)
                    waitForImportToFinish()
                }
                +"Import"
                id = CONFIRM_IMPORT_BUTTON_ID
                disabled = !canSubmit
            }
        }
    }
}