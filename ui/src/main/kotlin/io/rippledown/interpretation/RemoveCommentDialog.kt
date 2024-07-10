package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.interpretation.*
import io.rippledown.constants.main.CANCEL

interface RemoveCommentHandler {
    fun startRuleToRemoveComment(comment: String)
    fun cancel()
}

@Composable
fun RemoveCommentDialog(isShowing: Boolean, availableComments: List<String>, handler: RemoveCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(600.dp, 400.dp))

    var selectedComment by remember { mutableStateOf("") }

    DialogWindow(
        title = REMOVE_COMMENT,
        icon = painterResource("remove_comment_24.png"),
        state = dialogState,
        visible = isShowing,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val options = availableComments - selectedComment
            CommentSelector(
                selectedComment,
                options,
                "",
                REMOVE_COMMENT_SELECTOR_PREFIX,
                object : CommentSelectorHandler {
                    override fun onCommentSelected(comment: String) {
                        selectedComment = comment
                    }
                })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Define a common minimum width for buttons
                val buttonModifier = Modifier.widthIn(min = 100.dp).semantics { }
                Button(
                    onClick = {
                        handler.cancel()
                    },
                    modifier = buttonModifier.then(Modifier.semantics {
                        contentDescription = CANCEL_BUTTON_FOR_REMOVE_COMMENT
                    })
                ) {
                    Text(CANCEL)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        handler.startRuleToRemoveComment(selectedComment)
                    },
                    enabled = selectedComment.isNotBlank(),
                    modifier = Modifier.semantics {
                        contentDescription = OK_BUTTON_FOR_REMOVE_COMMENT
                    }
                ) {
                    Text(OK)
                }
            }
        }
    }
}