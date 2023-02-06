package io.rippledown.kb

import kotlinx.coroutines.launch
import mui.material.*
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.input
import react.useState
import web.file.File
import web.html.HTML.a
import web.html.HTMLInputElement
import web.html.InputType

val KBExportDialog = FC<KBHandler> { kbHandler ->
    var isOpen by useState(false)
    var canSubmit by useState(false)
    var selectedFile: File? by useState()

    Button {
        +"Export"
        id = "export_to_zip"
        variant = ButtonVariant.outlined
        size = Size.small
        onClick = {
            isOpen = true
        }
    }
    Dialog {
        id = "kb_export_dialog"
        open = isOpen
        DialogTitle {
            +"Export KB to a zip file"
        }
        DialogContent {
            id = "export_kb_dialog_content"
            DialogContentText {
                +"Select a folder into which the zip file should be saved"
            }
            input {
                type = InputType.file
                dir = ""
                id = "select_folder"
                name = "select_folder"
                onChange = {
                    if (it.target.files != null && it.target.files!!.length == 1) {
                        val sf = it.currentTarget.files!!.item(0)!!
                        selectedFile = sf
                        canSubmit = true
                    }
                }
            }
        }
        DialogActions {
            Button {
                onClick = { isOpen = false }
                +"Cancel"
                id = "cancel_zip_export"
            }
            a {
                + "Click here to download"
                href = kbHandler.api.exportURL()

            }
            Button {
                onClick = {
                    isOpen = false
                    mainScope.launch {
                        console.log("--------- about to call api to export to zip ----------------------")
                        kbHandler.api.exportKBToZip()
                    }
                }
                +"Export"
                id = "confirm_zip_export"
            }
        }
    }
}