package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
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
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.KBS_DROPDOWN_ID
import io.rippledown.model.KBInfo

interface KBControlHandler {
    var selectKB: (id: String) -> Unit
    var createKB: (name: String) -> Unit
    val kbList: () -> List<KBInfo>
}

@Composable
@Preview
fun KBControl(kbInfo: KBInfo?, handler: KBControlHandler) {


    var expanded by remember { mutableStateOf(false) }
    var createKbDialogShowing by remember { mutableStateOf(false) }
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
            CreateKB(object : CreateKBHandler {
                override var create: (name: String) -> Unit = { name ->
                    println("CreateKB dialog: OK called handler for kbName = $name")
                    handler.createKB(name)
                    createKbDialogShowing = false
                }

                override var cancel: () -> Unit = {
                    println("CreateKB dialog: Cancel called")
                    createKbDialogShowing = false
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
//                tint = colors.onPrimary
            )
        }

        Spacer(Modifier.width(4.dp))
        Text(
            text = kbName(),
//                color = colors.onPrimary,
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
            modifier = Modifier.testTag(KBS_DROPDOWN_ID)
                .semantics {
                    role = DropdownList
                    contentDescription = KBS_DROPDOWN_DESCRIPTION
                }
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    createKbDialogShowing = true
                },
                modifier = Modifier.testTag(CREATE_KB_ITEM_ID)
                    .semantics(mergeDescendants = true) {
                        role = Button
                        contentDescription = CREATE_KB_TEXT
                    }
            ) {
                Text(text = CREATE_KB_TEXT)
            }
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



