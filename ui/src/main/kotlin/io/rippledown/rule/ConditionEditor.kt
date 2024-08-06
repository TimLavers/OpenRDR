package io.rippledown.rule

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
                    Text(text = handler.editableCondition().fixedTextPart1())
                    OutlinedTextField(
                        value = enteredValue,
                        enabled = true,
                        onValueChange = {
                            enteredValue = it
                        },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    Text(text = handler.editableCondition().fixedTextPart2())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            handler.editingFinished(handler.editableCondition().condition(enteredValue))
                        },
//                        enabled = handler.isValidInput(textValue),
                        modifier = Modifier.semantics {
                            contentDescription = "content description"
                        }
                    ) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            handler.cancel()
                        },
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}