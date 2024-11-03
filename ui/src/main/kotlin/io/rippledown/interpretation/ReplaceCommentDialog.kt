package io.rippledown.interpretation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.components.OKCancelButtons
import io.rippledown.constants.interpretation.*
import openrdr.ui.generated.resources.Res.drawable
import openrdr.ui.generated.resources.replace_comment_24
import org.jetbrains.compose.resources.painterResource


interface ReplaceCommentHandler {
    fun startRuleToReplaceComment(toBeReplaced: String, replacement: String)
    fun cancel()
}

@Composable
fun ReplaceCommentDialog(givenComments: List<String>, availableComments: List<String>, handler: ReplaceCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 600.dp))
    var replacedText by remember { mutableStateOf("") }
    var replacementText by remember { mutableStateOf("") }
    val okButtonEnabled = givenComments.contains(replacedText)

    DialogWindow(
        title = REPLACE_COMMENT,
        icon = painterResource(drawable.replace_comment_24),
        state = dialogState,
        onCloseRequest = {
            handler.cancel()
        }
    ) {
        Scaffold(
            bottomBar = {
                OKCancelButtons(
                    prefix = REPLACEMENT_COMMENT_PREFIX,
                    oKButtonEnabled = okButtonEnabled,
                    onOK = {
                        handler.startRuleToReplaceComment(replacedText, replacementText)
                    },
                    onCancel = {
                        handler.cancel()
                    }
                )
            }

        ) { paddingValues ->
            val optionsForCommentToBeReplaced = givenComments - replacedText
            Column(Modifier.padding(paddingValues)) {
                CommentSelector(
                    replacedText,
                    options = optionsForCommentToBeReplaced,
                    REPLACED_COMMENT_LABEL,
                    REPLACED_COMMENT_PREFIX,
                    optionHeight = 100.dp,
                    modifier = Modifier.padding(all = 10.dp),
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            replacedText = comment
                        }
                    })

                Spacer(modifier = Modifier.height(10.dp))

                val optionsForReplacementComment = availableComments - replacementText - givenComments
                CommentSelector(
                    replacementText,
                    options = optionsForReplacementComment,
                    REPLACEMENT_COMMENT_LABEL,
                    REPLACEMENT_COMMENT_PREFIX,
                    optionHeight = 200.dp,
                    modifier = Modifier.padding(all = 10.dp),
                    object : CommentSelectorHandler {
                        override fun onCommentChanged(comment: String) {
                            replacementText = comment
                        }
                    })
            }
        }
    }
}