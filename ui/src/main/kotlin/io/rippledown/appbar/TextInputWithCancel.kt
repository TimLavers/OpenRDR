package io.rippledown.appbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.CANCEL

interface TextInputHandler {
    fun initialText() = ""
    fun isValidInput(input: String): Boolean
    fun labelText(): String
    fun inputFieldDescription(): String
    fun confirmButtonText(): String
    fun confirmButtonDescription(): String
    fun cancelButtonText() = CANCEL
    fun handleInput(value: String)
    fun cancel()
}

@Composable
fun TextInputWithCancel(handler: TextInputHandler) {
    var textValue by remember { mutableStateOf(handler.initialText()) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface {
        Box {
            Column(
                modifier = Modifier.padding(all = 4.dp)
            ) {
                OutlinedTextField(
                    value = textValue,
                    enabled = true,
                    onValueChange = { s ->
                        textValue = s
                    },
                    label = { Text(text = handler.labelText()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.8F, true)
                        .semantics {
                            contentDescription = handler.inputFieldDescription()
                        }
                        .focusRequester(focusRequester)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            handler.cancel()
                        },
                    ) {
                        Text(handler.cancelButtonText())
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            handler.handleInput(textValue)
                        },
                        enabled = handler.isValidInput(textValue),
                        modifier = Modifier.semantics {
                            contentDescription = handler.confirmButtonDescription()
                        }
                    ) {
                        Text(handler.confirmButtonText())
                    }
                }
            }
        }
    }
}
