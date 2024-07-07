package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.interpretation.*
import io.rippledown.constants.main.CANCEL

interface AddCommentHandler {
    fun startRuleToAddComment(comment: String)
    fun cancel()
}

@Composable
fun AddCommentDialog(isShowing: Boolean, handler: AddCommentHandler) {
    val dialogState = rememberDialogState(size = DpSize(640.dp, 200.dp))

    var textValue by remember { mutableStateOf("") }

    DialogWindow(
        title = ADD_COMMENT,
        icon = painterResource("add_comment_24.png"),
        state = dialogState,
        visible = isShowing,
        onCloseRequest = {
            handler.cancel()
        }
    ) {

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                enabled = true,
                maxLines = 4,
                onValueChange = { s ->
                    textValue = s
                },
                label = { Text(text = NEW_COMMENT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = NEW_COMMENT_TEXT_FIELD
                    }
                    .focusRequester(focusRequester)
            )
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
                        contentDescription = CANCEL_BUTTON_FOR_ADD_COMMENT
                    })
                ) {
                    Text(CANCEL)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        handler.startRuleToAddComment(textValue)
                    },
                    enabled = textValue.isNotBlank(),
                    modifier = buttonModifier.then(Modifier.semantics {
                        contentDescription = OK_BUTTON_FOR_ADD_COMMENT
                    })
                ) {
                    Text(OK)
                }
            }
        }
    }
}