package io.rippledown.rule

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.condition.edit.Type

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End

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
                        maxLines = 1,
                        onValueChange = {
                            enteredValue = it
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .semantics {
                                contentDescription = EDIT_CONDITION_FIELD_DESCRIPTION
                            }
                            .defaultMinSize(handler.editableCondition().editableValue().type.width())
                            .padding(2.dp)

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
                    Button(
                        onClick = {
                            handler.editingFinished(handler.editableCondition().condition(enteredValue))
                        },
                        enabled = handler.editableCondition().editableValue().type.valid(enteredValue),
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_OK_BUTTON_DESCRIPTION
                        }
                    ) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            handler.cancel()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
fun Type.width(): Dp {
    return when(this) {
        Type.Integer -> 15.dp
        Type.Real -> 20.dp
        Type.Text -> 400.dp
    }
}