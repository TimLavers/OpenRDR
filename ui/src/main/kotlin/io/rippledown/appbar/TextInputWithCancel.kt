package io.rippledown.appbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.*
import java.io.File

interface TextInputHandler {
    fun isValidInput(input: String): Boolean
    fun labelText(): String
    fun inputFieldDescription(): String
    fun confirmButtonText(): String
    fun confirmButtonDescription(): String
    fun cancelButtonText(): String
    fun handleInput(value: String)
    fun cancel()
}
const val TEXT_INPUT_FIELD_TEST_TAG = "text_input_id"
const val TEXT_INPUT_OK_BUTTON_TEST_TAG = "ok_button_id"
const val TEXT_INPUT_CANCEL_BUTTON_TEST_TAG = "cancel_button_id"
@Composable
fun TextInputWithCancel(handler: TextInputHandler) {
    var textValue by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
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
                            .testTag(TEXT_INPUT_FIELD_TEST_TAG)
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = handler.inputFieldDescription()
                            }
                            .focusRequester(focusRequester)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                handler.handleInput(textValue)
                            },
                            enabled = handler.isValidInput(textValue),
                            modifier = Modifier.testTag(TEXT_INPUT_OK_BUTTON_TEST_TAG)
                                .semantics {
                                    contentDescription = handler.confirmButtonDescription()
                                }
                        ) {
                            Text(handler.confirmButtonText())
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                handler.cancel()
                            },
                            modifier = Modifier.testTag(TEXT_INPUT_CANCEL_BUTTON_TEST_TAG)
                        ) {
                            Text(handler.cancelButtonText())
                        }
                    }
                }
            }
        }
    }
}
