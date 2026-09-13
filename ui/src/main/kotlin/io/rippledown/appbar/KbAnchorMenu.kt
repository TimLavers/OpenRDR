package io.rippledown.appbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import java.io.File

/**
 * Uses the current KB name as the anchor for import and export.
 * Other knowledge-base operations are available through the chat.
 */
@Composable
fun KbAnchorMenu(kbInfo: KBInfo?, handler: AppBarHandler) {
    var expanded by remember { mutableStateOf(false) }
    var importKbDialog by remember { mutableStateOf(false) }
    var exportKbDialog by remember { mutableStateOf(false) }

    if (importKbDialog) ImportKbDialog(
        onDismiss = { importKbDialog = false },
        onImport = { handler.importKB(it); importKbDialog = false }
    )
    if (exportKbDialog) ExportKbDialog(
        onDismiss = { exportKbDialog = false },
        onExport = { handler.exportKB(it); exportKbDialog = false }
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
private fun ImportKbDialog(onDismiss: () -> Unit, onImport: (File) -> Unit) {
    // 160dp clipped the Cancel / Import buttons off the bottom of the
    // dialog under Compose 1.11 (OutlinedTextField now reserves more
    // vertical space). Reserve enough room for the buttons row.
    val state = rememberDialogState(size = DpSize(640.dp, 240.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Import KB", state = state) {
        TextInputWithCancel(object : TextInputHandler {
            override fun isValidInput(input: String): Boolean {
                val file = File(input); return file.isFile && file.exists()
            }

            override fun labelText() = IMPORT_KB_NAME_FIELD_DESCRIPTION
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
    // See ImportKbDialog: 160dp clipped the buttons under Compose 1.11.
    val state = rememberDialogState(size = DpSize(640.dp, 240.dp))
    DialogWindow(onCloseRequest = onDismiss, title = "Export KB", state = state) {
        TextInputWithCancel(object : TextInputHandler {
            // Allow any non-blank path: `file.isFile` is true only if the
            // file already exists, so `isFile && !exists` was an
            // unreachable predicate (the OK button could never be enabled).
            override fun isValidInput(input: String) = input.isNotBlank()

            override fun labelText() = EXPORT_KB_NAME_FIELD_DESCRIPTION
            override fun inputFieldDescription() = EXPORT_KB_NAME_FIELD_DESCRIPTION
            override fun confirmButtonText() = EXPORT
            override fun confirmButtonDescription() = EXPORT_KB_OK_BUTTON_DESCRIPTION
            override fun handleInput(value: String) = onExport(File(value))
            override fun cancel() = onDismiss()
        })
    }
}
