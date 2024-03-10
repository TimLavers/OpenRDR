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

interface CreateKBHandler {
    var create: (name: String) -> Unit
    var cancel: () -> Unit
}

@Composable
fun CreateKB(handler: CreateKBHandler) {
    var kbName by remember { mutableStateOf("") }
    fun isNameOK() = kbName.isNotBlank()

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
                        value = kbName,
                        enabled = true,
                        onValueChange = { s ->
                            kbName = s
                        },
                        label = { Text(text = CREATE_KB_NAME) },
                        modifier = Modifier
                            .testTag(CREATE_KB_NAME_FIELD_ID)
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = CREATE_KB_NAME_FIELD_DESCRIPTION
                            }
                            .focusRequester(focusRequester)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                handler.create(kbName)
                            },
                            enabled = isNameOK(),
                            modifier = Modifier.testTag(CREATE_KB_OK_BUTTON_ID)
                                .semantics {
                                    contentDescription = CREATE_KB_OK_BUTTON_DESCRIPTION
                                }
                        ) {
                            Text(CREATE)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                handler.cancel()
                            },
                            modifier = Modifier.testTag(CREATE_KB_CANCEL_BUTTON_ID)
                        ) {
                            Text(CANCEL)
                        }
                    }
                }
            }
        }
    }
}
