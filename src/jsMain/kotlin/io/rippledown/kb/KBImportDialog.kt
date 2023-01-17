package io.rippledown.kb

import mui.material.*
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.input
import react.useState
import web.file.File
import web.timers.Timeout
import web.timers.clearInterval
import web.timers.setInterval

external interface KBImportDialogHandler: KBHandler {
    var reloadKB: () -> Unit
}

val KBImportDialog = FC<KBImportDialogHandler> {kbHandler ->
    var isOpen by useState(false)
    var canSubmit by useState(false)
    var selectedFile: File? by useState()
    var timerId: Timeout? = null
    fun waitForImportToFinish() {
        timerId = setInterval(   {
            val inProgress = kbHandler.api.importInProgress()
            if (!inProgress) {
                kbHandler.reloadKB()
                clearInterval(timerId!!)
            }
        }, 100)
    }

    Button {
        +"Import"
        id = "import_from_zip"
        variant = ButtonVariant.outlined
        size = Size.small
        onClick = {
            isOpen = true
        }
    }
    Dialog {
        open = isOpen
        DialogTitle {
            +"Import KB from zip file"
        }
        DialogContent {
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
                id = "cancel_zip_import"
            }
            Button {
                onClick = {
                    isOpen = false
                    kbHandler.api.importKBFromZip(selectedFile!!)
                    waitForImportToFinish()
                }
                +"Import"
                id = "confirm_zip_import"
                disabled = !canSubmit
            }
        }
    }
}