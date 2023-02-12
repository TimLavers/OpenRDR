import io.rippledown.model.Interpretation
import mui.icons.material.QuestionMark
import mui.material.*
import react.FC
import react.ReactNode
import react.useState

external interface ConclusionsDialogHandler : Handler {
    var interpretation: Interpretation
}

val ConclusionsDialog = FC<ConclusionsDialogHandler> { handler ->
    var isShowingConclusions by useState(false)
    Tooltip {
        title = "Why is this report given?".unsafeCast<ReactNode>()
        IconButton {
            QuestionMark {
            }
            id = "conclusions_dialog_open"
            onClick = {
                isShowingConclusions = true
            }
        }
    }
    Dialog {
        id = "conclusions_dialog"
        open = isShowingConclusions
        DialogTitle {
            +"Comments and conditions"
        }
        DialogContent {
            id = "conclusions_dialog_content"
            DialogContentText {
                +"Click a comment to see why it is part of this report."
            }
            ConclusionsView {
                interpretation = handler.interpretation
            }
        }
        DialogActions {
            Button {
                onClick = { isShowingConclusions = false }
                +"Close"
                id = "conclusions_dialog_close"
            }
        }
    }
}