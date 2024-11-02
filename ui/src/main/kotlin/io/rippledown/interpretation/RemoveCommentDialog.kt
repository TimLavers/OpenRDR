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
import io.rippledown.constants.interpretation.REMOVE_COMMENT
import io.rippledown.constants.interpretation.REMOVE_COMMENT_LABEL
import io.rippledown.constants.interpretation.REMOVE_COMMENT_PREFIX
import openrdr.ui.generated.resources.Res.drawable
import openrdr.ui.generated.resources.remove_comment_24
import org.jetbrains.compose.resources.painterResource

interface RemoveCommentHandler {
    fun startRuleToRemoveComment(comment: String)
    fun cancel()
}

@Composable
fun RemoveCommentDialog(givenComments: List<String>, handler: RemoveCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 400.dp))
    var selectedComment by remember { mutableStateOf("") }
    val okButtonEnabled = givenComments.contains(selectedComment)

    DialogWindow(
        title = REMOVE_COMMENT,
        icon = painterResource(drawable.remove_comment_24),
        state = dialogState,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Scaffold(
            bottomBar = {
                OKCancelButtons(
                    prefix = REMOVE_COMMENT_PREFIX,
                    oKButtonEnabled = okButtonEnabled,
                    onOK = {
                        handler.startRuleToRemoveComment(selectedComment)
                    },
                    onCancel = {
                        handler.cancel()
                    }
                )
            }
        ) { paddingValues ->
            val options = givenComments - selectedComment
            Column(Modifier.padding(paddingValues)) {
                CommentSelector(
                    selectedComment,
                    options,
                    REMOVE_COMMENT_LABEL,
                    REMOVE_COMMENT_PREFIX,
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