package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.components.OKCancelButtons
import io.rippledown.constants.interpretation.ADD_COMMENT
import io.rippledown.constants.interpretation.ADD_COMMENT_LABEL
import io.rippledown.constants.interpretation.ADD_COMMENT_PREFIX
import openrdr.ui.generated.resources.Res
import openrdr.ui.generated.resources.add_comment_24
import org.jetbrains.compose.resources.painterResource

interface AddCommentHandler {
    fun startRuleToAddComment(comment: String)
    fun cancel()
}

@Composable
fun AddCommentDialog(availableComments: List<String>, handler: AddCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 400.dp))
    var selectedComment by remember { mutableStateOf("") }

    DialogWindow(
        title = ADD_COMMENT,
        icon = painterResource(Res.drawable.add_comment_24),
        state = dialogState,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Scaffold(
            bottomBar = {
                OKCancelButtons(
                    prefix = ADD_COMMENT_PREFIX,
                    oKButtonEnabled = true,
                    onOK = {
                        handler.startRuleToAddComment(selectedComment)
                    },
                    onCancel = {
                        handler.cancel()
                    }
                )
            }
        ) { paddingValues ->
            val options = availableComments - selectedComment

            Column(Modifier.padding(paddingValues)) {
                CommentSelector(
                    selectedComment,
                    options,
                    ADD_COMMENT_LABEL,
                    ADD_COMMENT_PREFIX,
                    200.dp,
                    modifier = Modifier.padding(all = 10.dp),
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            selectedComment = comment
                        }
                    })
            }
        }
    }
}