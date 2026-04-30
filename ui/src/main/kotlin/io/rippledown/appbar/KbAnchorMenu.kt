package io.rippledown.appbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.semantics.Role.Companion.DropdownList
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.interpretation.OK
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher
import java.io.File

/**
 * Unified knowledge-base anchor menu, modelled on the IntelliJ project switcher.
 *
 * The current KB name itself is the trigger: clicking it opens a single dropdown
 * containing edit-current-KB actions, KB-management actions, and a flat list of
 * other KBs to switch to. There is no separate "Knowledge Bases" or "Edit"
 * button.
 */
@Composable
fun KbAnchorMenu(kbInfo: KBInfo?, handler: AppBarHandler) {
    var expanded by remember { mutableStateOf(false) }
    var createKbDialog by remember { mutableStateOf(false) }
    var createKbFromSampleDialog by remember { mutableStateOf(false) }
    var importKbDialog by remember { mutableStateOf(false) }
    var exportKbDialog by remember { mutableStateOf(false) }
    var kbDescriptionDialog by remember { mutableStateOf(false) }

    val availableKBs = remember { mutableStateListOf<KBInfo>() }
    LaunchedEffect(kbInfo) {
        withContext(MainUIDispatcher) {
            val others = handler.kbList().filter { it != kbInfo }.sorted()
            availableKBs.clear()
            availableKBs.addAll(others)
        }
    }

    if (createKbDialog) CreateKbDialog(
        onDismiss = { createKbDialog = false },
        onCreate = { handler.createKB(it); createKbDialog = false }
    )
    if (createKbFromSampleDialog) CreateKbFromSampleDialog(
        onDismiss = { createKbFromSampleDialog = false },
        onCreate = { name, sample ->
            handler.createKBFromSample(name, sample); createKbFromSampleDialog = false
        }
    )
    if (importKbDialog) ImportKbDialog(
        onDismiss = { importKbDialog = false },
        onImport = { handler.importKB(it); importKbDialog = false }
    )
    if (exportKbDialog) ExportKbDialog(
        onDismiss = { exportKbDialog = false },
        onExport = { handler.exportKB(it); exportKbDialog = false }
    )
    if (kbDescriptionDialog) KbDescriptionDialog(
        initialDescription = handler.kbDescription(),
        onDismiss = { kbDescriptionDialog = false },
        onSave = { handler.setKbDescription(it); kbDescriptionDialog = false }
    )

    Box(
        Modifier
            .semantics { contentDescription = KB_CONTROL_DESCRIPTION }
            .testTag(KB_CONTROL_ID)
    ) {
        KbAnchorTrigger(
            kbName = kbInfo?.name ?: NO_KB_SELECTED,
            onClick = { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.semantics {
                role = DropdownList
                contentDescription = KBS_DROPDOWN_DESCRIPTION
            }
        ) {
            // Edit current KB
            MenuItem(
                text = EDIT_KB_DESCRIPTION_BUTTON_TEXT,
                description = EDIT_KB_DESCRIPTION_BUTTON_TEXT,
                onClick = { expanded = false; kbDescriptionDialog = true }
            )

            Divider(startIndent = 2.dp, thickness = 1.dp)

            // KB management
            MenuItem(
                text = CREATE_KB_TEXT,
                description = CREATE_KB_TEXT,
                onClick = { expanded = false; createKbDialog = true }
            )
            MenuItem(
                text = CREATE_KB_FROM_SAMPLE_TEXT,
                description = CREATE_KB_FROM_SAMPLE_TEXT,
                onClick = { expanded = false; createKbFromSampleDialog = true }
            )
            MenuItem(
                text = IMPORT_KB_TEXT,
                description = IMPORT_KB_TEXT,
                onClick = { expanded = false; importKbDialog = true }
            )
            MenuItem(
                text = EXPORT_KB_TEXT,
                description = EXPORT_KB_TEXT,
                onClick = { expanded = false; exportKbDialog = true }
            )

            // Switch KB list (other KBs only)
            if (availableKBs.isNotEmpty()) {
                Divider(startIndent = 2.dp, thickness = 1.dp)
                SwitchKbHeader()
                availableKBs.forEach { kbi ->
                    KbInfoItem(kbi.name, object : KbSelectionHandler {
                        override var onSelect: () -> Unit = {
                            handler.selectKB(kbi.id); expanded = false
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun KbAnchorTrigger(kbName: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = colors.onPrimary),
        modifier = Modifier.semantics {
            contentDescription = KB_CONTROL_DROPDOWN_DESCRIPTION
        }
    ) {
        Text(
            text = kbName,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            color = colors.onPrimary,
            modifier = Modifier
                .testTag(KB_NAME_ID)
                .semantics(mergeDescendants = true) {
                    contentDescription = KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
                }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = colors.onPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuItem(
    text: String,
    description: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier
            .height(28.dp)
            .semantics(mergeDescendants = true) {
                role = Button
                contentDescription = description
            }
    ) {
        Text(text = text, style = MaterialTheme.typography.body2)
    }
}

@Composable
private fun SwitchKbHeader() {
    Text(
        text = SWITCH_KB_HEADER_TEXT,
        style = MaterialTheme.typography.caption,
        color = Color.Gray,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

@Composable
private fun CreateKbDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val state = rememberDialogState(size = DpSize(420.dp, 160.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Create KB", state = state) {
        TextInputWithCancel(object : TextInputHandler {
            override fun handleInput(value: String) = onCreate(value)
            override fun cancel() = onDismiss()
            override fun isValidInput(input: String) = input.isNotBlank()
            override fun labelText() = CREATE_KB_NAME
            override fun inputFieldDescription() = CREATE_KB_NAME_FIELD_DESCRIPTION
            override fun confirmButtonText() = CREATE
            override fun confirmButtonDescription() = CREATE_KB_OK_BUTTON_DESCRIPTION
        })
    }
}

@Composable
private fun CreateKbFromSampleDialog(
    onDismiss: () -> Unit,
    onCreate: (String, SampleKB) -> Unit
) {
    val state = rememberDialogState(size = DpSize(640.dp, 500.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Create KB from Template", state = state) {
        CreateKBFromSample(object : CreateKBFromSampleHandler {
            override fun createKB(name: String, sample: SampleKB) = onCreate(name, sample)
            override fun cancel() = onDismiss()
        })
    }
}

@Composable
private fun ImportKbDialog(onDismiss: () -> Unit, onImport: (File) -> Unit) {
    val state = rememberDialogState(size = DpSize(640.dp, 160.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Import KB", state = state) {
        TextInputWithCancel(object : TextInputHandler {
            override fun isValidInput(input: String): Boolean {
                val file = File(input); return file.isFile && file.exists()
            }

            override fun labelText() = IMPORT_KB_TEXT
            override fun inputFieldDescription() = IMPORT_KB_NAME_FIELD_DESCRIPTION
            override fun confirmButtonText() = IMPORT
            override fun confirmButtonDescription() = IMPORT_KB_OK_BUTTON_DESCRIPTION
            override fun handleInput(value: String) = onImport(File(value))
            override fun cancel() = onDismiss()
        })
    }
}

@Composable
private fun ExportKbDialog(onDismiss: () -> Unit, onExport: (File) -> Unit) {
    val state = rememberDialogState(size = DpSize(640.dp, 160.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Export KB", state = state) {
        TextInputWithCancel(object : TextInputHandler {
            override fun isValidInput(input: String): Boolean {
                val file = File(input); return file.isFile && !file.exists()
            }

            override fun labelText() = EXPORT_KB_TEXT
            override fun inputFieldDescription() = EXPORT_KB_NAME_FIELD_DESCRIPTION
            override fun confirmButtonText() = EXPORT
            override fun confirmButtonDescription() = EXPORT_KB_OK_BUTTON_DESCRIPTION
            override fun handleInput(value: String) = onExport(File(value))
            override fun cancel() = onDismiss()
        })
    }
}

@Composable
private fun KbDescriptionDialog(
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val state = rememberDialogState(size = DpSize(640.dp, 460.dp))
    DialogWindow(onCloseRequest = onDismiss, title = EDIT_KB_DESCRIPTION_TEXT, state = state) {
        TextInputWithCancel(object : TextInputHandler {
            override fun isValidInput(input: String) = true
            override fun initialText() = initialDescription
            override fun labelText() = ""
            override fun inputFieldDescription() = EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION
            override fun confirmButtonText() = OK
            override fun confirmButtonDescription() = EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION
            override fun handleInput(value: String) = onSave(value)
            override fun cancel() = onDismiss()
        })
    }
}

