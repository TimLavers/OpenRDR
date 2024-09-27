package io.rippledown.rule

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
import io.rippledown.constants.main.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.EditableCondition

interface ConditionEditHandler {
    fun editableCondition(): EditableCondition
    fun editingFinished(condition: Condition)
    fun cancel()
}

@Composable
fun ConditionEditor(handler: ConditionEditHandler) {
    var enteredValue by remember { mutableStateOf(handler.editableCondition().editableValue().value) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }


    Surface {
        Box {
            Column(
                modifier = Modifier.padding(all = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = handler.editableCondition().fixedTextPart1(),
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_TEXT_1_DESCRIPTION
                        }
                    )
                    OutlinedTextField(
                        value = enteredValue,
                        enabled = true,
                        onValueChange = {
                            enteredValue = it
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .semantics {
                                contentDescription = EDIT_CONDITION_FIELD_DESCRIPTION
                            }
                    )
                    Text(
                        text = handler.editableCondition().fixedTextPart2(),
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_TEXT_2_DESCRIPTION
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            handler.cancel()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION
                        }
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            handler.editingFinished(handler.editableCondition().condition(enteredValue))
                        },
//                        enabled = handler.isValidInput(textValue),
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_OK_BUTTON_DESCRIPTION
                        }
                    ) {
                        Text("OK")
                    }

                }
            }
        }
    }
}