package io.rippledown.appbar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rippledown.constants.main.*
import io.rippledown.sample.SampleKB

interface CreateKBFromSampleHandler {
    fun createKB(name: String, sample: SampleKB)
    fun cancel()
}

// todo refactor TextInputWithCancel as a ConfirmOrCancel control...
@Composable
fun CreateKBFromSample(handler: CreateKBFromSampleHandler) {
    var textValue by remember { mutableStateOf("") }
    var selectedSampleKB by remember { mutableStateOf(SampleKB.TSH) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface {
        Box {
            Column(
                modifier = Modifier.padding(all = 4.dp)
            ) {
                Text(
                    style = MaterialTheme.typography.h6,
                    text = SELECT_SAMPLE,
                    color = MaterialTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                        .semantics {
                            contentDescription = SELECT_SAMPLE
                        }
                )
                TemplateChooser { s ->
                    selectedSampleKB = s
                }
                OutlinedTextField(
                    value = textValue,
                    enabled = true,
                    onValueChange = { s ->
                        textValue = s
                    },
                    label = { Text(text = CREATE_KB_FROM_SAMPLE_NAME) },
                    modifier = Modifier
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
                    TextButton(
                        onClick = {
                            handler.cancel()
                        },
                    ) {
                        Text(CANCEL)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            handler.createKB(textValue, selectedSampleKB)
                        },
                        enabled = textValue.isNotBlank(),
                        modifier = Modifier.semantics {
                            contentDescription = CREATE_KB_OK_BUTTON_DESCRIPTION
                        }
                    ) {
                        Text(CREATE)
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateChooser(onSelect: (SampleKB) -> Unit) {
    // See https://foso.github.io/Jetpack-Compose-Playground/material/radiobutton/
    var selectedSampleKB by remember { mutableStateOf(SampleKB.TSH) }
    Column {
        SampleKB.entries.forEach {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (it == selectedSampleKB),
                        onClick = {
                            onSelect(it)
                            selectedSampleKB = it
                        }
                    )
                    .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (it == selectedSampleKB),
                    onClick = {
                        onSelect(it)
                        selectedSampleKB = it
                    },
                    modifier = Modifier.semantics {
                        contentDescription = it.radioButtonDescription()
                    }
                )
                Text(
                    text = it.title(),
                    style = MaterialTheme.typography.body1.merge(),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

fun SampleKB.radioButtonDescription() = "select ${title()}"