package io.rippledown.kb

import js.core.get
import react.dom.html.ReactHTML.input
import mui.material.*
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.label
import react.useState
import web.buffer.Blob
import web.file.File
import web.file.FileReader

val KBImportDialog = FC<KBHandler> {kbHandler ->
    var isOpen by useState(false)
    var canSubmit by useState(false)
    var selectedFile by useState<File>()

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
                    println("---- on change. Event: ${it.target.files}")
                    if (it.target.files != null) {
                        println("++++++++++++++++ ${it.target.files!!.get(0).name}")
                    }
                    println("---- on change. Event: ${it.currentTarget.files}")
                    selectedFile = it.currentTarget.files?.item(0)
                    println("Selected file: $selectedFile")
                    if (selectedFile != null && selectedFile!!.name.endsWith("zip")) {
                        kbHandler.api.importKBFromZip(selectedFile!!)
                        canSubmit = true
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
                }
                +"Import"
                id = "confirm_zip_import"
                disabled = !canSubmit
            }
        }
    }
}