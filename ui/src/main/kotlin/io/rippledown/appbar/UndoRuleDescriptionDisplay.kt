package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sun.org.apache.xalan.internal.lib.ExsltStrings.padding
import io.rippledown.constants.kb.*
import io.rippledown.model.rule.UndoRuleDescription

interface UndoRuleDescriptionDisplayHandler {
    fun description(): UndoRuleDescription
    fun cancel()
    fun undoLastRule()
}

@Composable
@Preview
fun UndoRuleDescriptionDisplay(handler: UndoRuleDescriptionDisplayHandler) {
    var undoRuleClicked by remember { mutableStateOf(false) }
    val udr = handler.description()
    Surface {
        Box {
            Column(
                modifier = Modifier.padding(all = 4.dp)
            ) {
                Row {
                    OutlinedTextField(
                        value = udr.description,
                        onValueChange = {},
                        label = {
                            Text(
                                text = "Last rule",
                                style = TextStyle(fontStyle = FontStyle.Italic),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8F)
                            .testTag(LAST_RULE_DESCRIPTION_ID)
                            .semantics { contentDescription = LAST_RULE_DESCRIPTION_DESCRIPTION }
                    )
                }
                if (!undoRuleClicked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                handler.cancel()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = CLOSE_SHOW_LAST_RULE_DESCRIPTION
                            }
                        ) {
                            Text("Close")
                        }
                        if (udr.canRemove) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    undoRuleClicked = true
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = UNDO_LAST_RULE_BUTTON_DESCRIPTION
                                }
                            ) {
                                Text("Undo last rule")
                            }
                        }
                    }
                }
                if (undoRuleClicked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = CONFIRM_UNDO_LAST_RULE_TEXT,
                            modifier = Modifier.padding(13.dp)
                                .semantics {
                                contentDescription = CONFIRM_UNDO_LAST_RULE_TEXT_DESCRIPTION
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                handler.undoLastRule()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = CONFIRM_UNDO_LAST_RULE_YES_OPTION_DESCRIPTION
                            }
                        ) {
                            Text("Yes")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                undoRuleClicked = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = CONFIRM_UNDO_LAST_RULE_NO_OPTION_DESCRIPTION
                            }
                        ) {
                            Text("No")
                        }
                    }
                }
            }
        }
    }
}
