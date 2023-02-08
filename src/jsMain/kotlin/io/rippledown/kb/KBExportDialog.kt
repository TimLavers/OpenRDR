package io.rippledown.kb

import mui.material.*
import react.FC
import react.useState

val KBExportDialog = FC<KBHandler> { kbHandler ->
    var isOpen by useState(false)

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
                +"The Knowledge Base will be saved as a zip file in your downloads directory."
            }
        }
        DialogActions {
            Button {
                onClick = { isOpen = false }
                +"Cancel"
                id = "cancel_zip_export"
            }
            Link {
                + "Export"
                component = Button
                underline = LinkUnderline.none
                href = kbHandler.api.exportURL()
                id = "confirm_zip_export"
                onClick = { isOpen = false }
            }
        }
    }
}