package io.rippledown.kb

import io.rippledown.constants.kb.CANCEL_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.CONFIRM_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_EXPORT_BUTTON_ID
import io.rippledown.main.Handler
import mui.icons.material.FileDownload
import mui.material.*
import react.FC
import react.ReactNode
import react.useState

external interface ExportKBHandler : Handler {
    var onFinish: () -> Unit
}

val ExportKB = FC<ExportKBHandler> { handler ->
    var isOpen by useState(false)

    Tooltip {
        title = "Export knowledge base to file".unsafeCast<ReactNode>()
        MenuItem {
            id = KB_EXPORT_BUTTON_ID
            ListItemIcon {
                FileDownload {
                }
            }
            ListItemText {
                primary = "Export".unsafeCast<ReactNode>()
            }
            onClick = {
                isOpen = true
            }
        }
    }
    Dialog {
        id = "kb_export_dialog"
        open = isOpen
        DialogTitle {
            "Export KB to a zip file".unsafeCast<ReactNode>()
        }
        DialogContent {
            id = "export_kb_dialog_content"
            DialogContentText {
                "The Knowledge Base will be saved as a zip file in your downloads directory.".unsafeCast<ReactNode>()
            }
        }
        DialogActions {
            Button {
                id = CANCEL_EXPORT_BUTTON_ID
                onClick = {
                    isOpen = false
                    handler.onFinish()
                }
                +"Cancel"
            }
            Link {
                id = CONFIRM_EXPORT_BUTTON_ID
                component = Button
                underline = LinkUnderline.none
                href = handler.api.exportURL()
                onClick = {
                    isOpen = false
                    handler.onFinish()
                }
                +"Export"
            }
        }
    }
}