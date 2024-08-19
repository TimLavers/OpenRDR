package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import io.rippledown.constants.interpretation.*

interface ReplaceCommentHandler {
    fun startRuleToReplaceComment(toBeReplaced: String, replacement: String)
    fun cancel()
}

@Composable
fun ReplaceCommentDialog(availableComments: List<String>, handler: ReplaceCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 600.dp))
    var replacedText by remember { mutableStateOf("") }
    var replacementText by remember { mutableStateOf("") }

    DialogWindow(
        title = REPLACE_COMMENT,
        icon = painterResource("replace_comment_24.png"),
        state = dialogState,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Scaffold(
            bottomBar = {
                OKCancelButtons(
                    onOK = {
                        handler.startRuleToReplaceComment(replacedText, replacementText)
                    },
                    onCancel = {
                        handler.cancel()
                    },
                    prefix = REPLACE_COMMENT_PREFIX
                )
            }

        ) { paddingValues ->
            val options = availableComments - replacedText
            Column(Modifier.padding(paddingValues)) {
                CommentSelector(
                    replacedText,
                    options,
                    COMMENT_TO_BE_REPLACED,
                    REPLACE_COMMENT_PREFIX,
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            replacedText = comment
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                CommentSelector(
                    replacementText,
                    options,
                    COMMENT_TO_BE_THE_REPLACEMENT,
                    REPLACEMENT_COMMENT_PREFIX,
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            replacementText = comment
                        }
                    }
                )
            }
        }
    }
}