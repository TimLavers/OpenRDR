package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.semantics.Role.Companion.DropdownList
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign.Companion.Start
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import java.io.File

interface KBControlHandler {
    var selectKB: (id: String) -> Unit
    var createKB: (name: String) -> Unit
    var createKBFromSample: (name: String, sample: SampleKB) -> Unit
    var importKB: (data: File) -> Unit
    var exportKB: (data: File) -> Unit
    val kbList: () -> List<KBInfo>
}

@Composable
@Preview
fun KBControl(kbInfo: KBInfo?, handler: KBControlHandler) {
    var expanded by remember { mutableStateOf(false) }
    var createKbDialogShowing by remember { mutableStateOf(false) }
    var createKbFromSampleDialogShowing by remember { mutableStateOf(false) }
    var importKbDialogShowing by remember { mutableStateOf(false) }
    var exportKbDialogShowing by remember { mutableStateOf(false) }
    val availableKBs =
        remember { mutableStateListOf<KBInfo>() } // https://tigeroakes.com/posts/mutablestateof-list-vs-mutablestatelistof/

    fun kbName() = kbInfo?.name ?: ""

    LaunchedEffect(Unit) {
        val kbsApartFromCurrent = handler.kbList().filter { it != kbInfo }.sorted()
        availableKBs.clear()
        availableKBs.addAll(kbsApartFromCurrent)
    }

    if (createKbDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(420.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { createKbDialogShowing = false },
            title = "Create KB",
            state = dialogState,
        ) {
            TextInputWithCancel(object : TextInputHandler {
                override fun handleInput(value: String) {
                    handler.createKB(value)
                    createKbDialogShowing = false
                }

                override fun cancel() {
                    createKbDialogShowing = false
                }

                override fun isValidInput(input: String) = input.isNotBlank()
                override fun labelText() = CREATE_KB_NAME
                override fun inputFieldDescription() = CREATE_KB_NAME_FIELD_DESCRIPTION
                override fun confirmButtonText() = CREATE
                override fun confirmButtonDescription() = CREATE_KB_OK_BUTTON_DESCRIPTION
            })
        }
    }
    if (createKbFromSampleDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(640.dp, 500.dp))
        DialogWindow(
            onCloseRequest = {createKbFromSampleDialogShowing = false},
            title = "Create KB from Template",
            state = dialogState
        ) {
            CreateKBFromSample(object : CreateKBFromSampleHandler {
                override fun createKB(name: String, sample: SampleKB) {
                    println("Create KB from sample....name: $name, sample: $sample")
                    handler.createKBFromSample(name, sample)
                    createKbFromSampleDialogShowing = false
                }

                override fun cancel() {
                    createKbFromSampleDialogShowing = false
                }
            })
        }
    }
    if (importKbDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(640.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { importKbDialogShowing = false },
            title = "Import KB",
            state = dialogState,
        ) {
            TextInputWithCancel(object : TextInputHandler {
                override fun isValidInput(input: String): Boolean {
                    val file = File(input)
                    return file.isFile && file.exists()
                }

                override fun labelText() = IMPORT_KB_TEXT
                override fun inputFieldDescription() = IMPORT_KB_NAME_FIELD_DESCRIPTION
                override fun confirmButtonText() = IMPORT
                override fun confirmButtonDescription() = IMPORT_KB_OK_BUTTON_DESCRIPTION
                override fun handleInput(value: String) {
                    handler.importKB(File(value))
                    importKbDialogShowing = false
                }

                override fun cancel() {
                    importKbDialogShowing = false
                }
            })
        }
    }
    if (exportKbDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(640.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { exportKbDialogShowing = false },
            title = "Export KB",
            state = dialogState,
        ) {
            TextInputWithCancel(object : TextInputHandler {
                override fun isValidInput(input: String): Boolean {
                    val file = File(input)
                    return file.isFile && !file.exists()
                }

                override fun labelText() = EXPORT_KB_TEXT
                override fun inputFieldDescription() = EXPORT_KB_NAME_FIELD_DESCRIPTION
                override fun confirmButtonText() = EXPORT
                override fun confirmButtonDescription() = EXPORT_KB_OK_BUTTON_DESCRIPTION
                override fun handleInput(value: String) {
                    handler.exportKB(File(value))
                    exportKbDialogShowing = false
                }

                override fun cancel() {
                    exportKbDialogShowing = false
                }
            })
        }
    }
    Row(
        Modifier
            .semantics {
                contentDescription = KB_CONTROL_DESCRIPTION
            }
            .padding(16.dp)
            .testTag(KB_CONTROL_ID)
    ) {

        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = KB_CONTROL_DROPDOWN_DESCRIPTION
            }
        ) {
            Icon(
                imageVector = Default.KeyboardArrowDown,
                contentDescription = KB_CONTROL_DROPDOWN_DESCRIPTION,
                tint = colors.onPrimary
            )
        }

        Spacer(Modifier.width(4.dp))
        Text(
            text = kbName(),
            color = colors.onPrimary,
            textAlign = Start,
            modifier = Modifier
                .weight(1f)
                .testTag(KB_SELECTOR_ID)
                .semantics {
                    contentDescription = KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.semantics {
                    role = DropdownList
                    contentDescription = KBS_DROPDOWN_DESCRIPTION
                }
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    createKbDialogShowing = true
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                        role = Button
                        contentDescription = CREATE_KB_TEXT
                    }
            ) {
                Text(text = CREATE_KB_TEXT)
            }
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    createKbFromSampleDialogShowing = true
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                        role = Button
                        contentDescription = CREATE_KB_FROM_SAMPLE_TEXT
                    }
            ) {
                Text(text = CREATE_KB_FROM_SAMPLE_TEXT)
            }
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    importKbDialogShowing = true
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                        role = Button
                        contentDescription = IMPORT_KB_TEXT
                    }
            ) {
                Text(text = IMPORT_KB_TEXT)
            }
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    exportKbDialogShowing = true
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                        role = Button
                        contentDescription = EXPORT_KB_TEXT
                    }
            ) {
                Text(text = EXPORT_KB_TEXT)
            }
            Divider(startIndent = 2.dp, thickness = 1.dp)
            availableKBs.forEach { kbi ->
                KbInfoItem(kbi.name, object : KbSelectionHandler {
                    override var onSelect: () -> Unit = {
                        handler.selectKB(kbi.id)
                        expanded = false
                    }
                })
            }
        }
    }
}



