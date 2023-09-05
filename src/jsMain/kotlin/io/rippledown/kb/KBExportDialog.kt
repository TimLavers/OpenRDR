package io.rippledown.kb

import Handler
import io.rippledown.constants.kb.CANCEL_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.CONFIRM_EXPORT_BUTTON_ID
import io.rippledown.constants.kb.KB_EXPORT_BUTTON_ID
import mui.material.*
import react.FC
import react.ReactNode
import react.useState

val KBExportDialog = FC<Handler> { handler ->
    var isOpen by useState(false)

    Button {
        +"Export"
        id = KB_EXPORT_BUTTON_ID
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
                +"Cancel"
                id = CANCEL_EXPORT_BUTTON_ID
                onClick = { isOpen = false }
            }
            Link {
                +"Export"
                id = CONFIRM_EXPORT_BUTTON_ID
                component = Button
                underline = LinkUnderline.none
                href = handler.api.exportURL()
                onClick = { isOpen = false }
            }
        }
    }
}