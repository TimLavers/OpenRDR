package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.Role.Companion.DropdownList
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import java.io.File

interface KbEditControlHandler {
    var setKbDescription: (name: String) -> Unit
    var kbDescription: () -> String
}

@Composable
@Preview
fun EditCurrentKbControl(handler: KbEditControlHandler) {
    var expanded by remember { mutableStateOf(false) }
    var kbDescriptionDialogShowing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
    }

    if (kbDescriptionDialogShowing) {
        val dialogState = rememberDialogState(size = DpSize(640.dp, 460.dp))
        DialogWindow(
            onCloseRequest = { kbDescriptionDialogShowing = false },
            title = EDIT_KB_DESCRIPTION_TEXT,
            state = dialogState,
        ) {
            TextInputWithCancel(object : TextInputHandler {
                override fun isValidInput(input: String) = true
                override fun initialText() = handler.kbDescription()
                override fun labelText() = EDIT_KB_DESCRIPTION_TEXT
                override fun inputFieldDescription() = EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION
                override fun confirmButtonText() = EDIT
                override fun confirmButtonDescription() = EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION
                override fun handleInput(value: String) {
                    handler.setKbDescription(value)
                    kbDescriptionDialogShowing = false
                }

                override fun cancel() {
                    kbDescriptionDialogShowing = false
                }
            })
        }
    }

    Box(
        Modifier
            .semantics {
                contentDescription = EDIT_CURRENT_KB_CONTROL_DESCRIPTION
            }
//            .padding(16.dp)
            .testTag(EDIT_CURRENT_KB_CONTROL_ID)
    ) {

        Button(
            onClick = { expanded = true },
            modifier = Modifier.semantics {
                contentDescription = "KB Edit Control Button"
            }
        ) {
//            Icon(
//                imageVector = Default.Edit,
//                contentDescription = EDIT_CURRENT_KB_CONTROL_DROPDOWN_DESCRIPTION,
//                tint = colors.onPrimary
//            )
            Text(text = EDIT)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.semantics {
                role = DropdownList
                contentDescription = EDIT_CURRENT_KB_CONTROL_DROPDOWN_DESCRIPTION
            }
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    kbDescriptionDialogShowing = true
                },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = EDIT_KB_DESCRIPTION_BUTTON_TEXT
                }
            ) {
                Text(text = EDIT_KB_DESCRIPTION_BUTTON_TEXT)
            }
        }
    }
}
