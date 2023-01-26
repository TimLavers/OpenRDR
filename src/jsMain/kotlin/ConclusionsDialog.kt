import io.rippledown.model.Interpretation
import mui.material.*
import react.FC
import react.useState

external interface ConclusionsDialogHandler : Handler {
    var interpretation: Interpretation
}

val ConclusionsDialog = FC<ConclusionsDialogHandler> { handler ->
    var isOpen by useState(true)

    Dialog {
        id = "conclusions_dialog"
        open = isOpen
        DialogTitle {
            +"Comments and conditions"
        }
        DialogContent {
            id = "conclusions_dialog_content"
            DialogContentText {
                +"Click a comment to see the conditions"
            }
        }
        DialogActions {
            Button {
                onClick = { isOpen = false }
                +"Close"
                id = "close_conclusions_dialog"
            }
        }
    }
}