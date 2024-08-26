package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.components.OKCancelButtons
import io.rippledown.constants.interpretation.REMOVE_COMMENT
import io.rippledown.constants.interpretation.REMOVE_COMMENT_LABEL
import io.rippledown.constants.interpretation.REMOVE_COMMENT_PREFIX

interface RemoveCommentHandler {
    fun startRuleToRemoveComment(comment: String)
    fun cancel()
}

@Composable
fun RemoveCommentDialog(availableComments: List<String>, handler: RemoveCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 400.dp))
    var selectedComment by remember { mutableStateOf("") }
    DialogWindow(
        title = REMOVE_COMMENT,
        icon = painterResource("remove_comment_24.png"),
        state = dialogState,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Scaffold(
            bottomBar = {
                OKCancelButtons(
                    onOK = {
                        handler.startRuleToRemoveComment(selectedComment)
                    },
                    onCancel = {
                        handler.cancel()
                    },
                    prefix = REMOVE_COMMENT_PREFIX
                )
            }
        ) { paddingValues ->
            val options = availableComments - selectedComment
            Column(Modifier.padding(paddingValues)) {
                CommentSelector(
                    selectedComment,
                    options,
                    REMOVE_COMMENT_LABEL,
                    REMOVE_COMMENT_PREFIX,
                    200.dp,
                    modifier = Modifier,
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            selectedComment = comment
                        }
                    })
            }
        }
    }
}