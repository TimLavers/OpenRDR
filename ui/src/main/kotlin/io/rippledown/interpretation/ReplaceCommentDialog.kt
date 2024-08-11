package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
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

interface ReplaceCommentHandler {
    fun startRuleToReplaceComment(toBeReplaced: String, replacement: String)
    fun cancel()
}

@Composable
fun ReplaceCommentDialog(availableComments: List<String>, handler: ReplaceCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 250.dp))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val options = availableComments - replacedText
            CommentSelector(
                replacedText,
                options,
                COMMENT_TO_BE_REPLACED,
                REPLACE_COMMENT_SELECTOR_PREFIX,
                object : CommentSelectorHandler {
                    override fun onCommentSelected(comment: String) {
                        replacedText = comment
                    }
                })
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = replacementText,
                enabled = true,
                maxLines = 4,
                onValueChange = { s ->
                    replacementText = s
                },
                label = { Text(text = COMMENT_TO_BE_THE_REPLACEMENT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = REPLACEMENT_COMMENT_TEXT_FIELD
                    }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Define a common minimum width for buttons
                val buttonModifier = Modifier.widthIn(min = 100.dp)
                Button(
                    onClick = {
                        handler.cancel()
                    },
                    modifier = buttonModifier.then(Modifier.semantics {
                        contentDescription = CANCEL_BUTTON_FOR_REPLACE_COMMENT
                    })
                ) {
                    Text(CANCEL)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        handler.startRuleToReplaceComment(replacedText, replacementText)
                    },
                    enabled = replacementText.isNotBlank() && replacedText.isNotBlank(),
                    modifier = buttonModifier.then(Modifier.semantics {
                        contentDescription = OK_BUTTON_FOR_REPLACE_COMMENT
                    })
                ) {
                    Text(OK)
                }
            }
        }
    }
}