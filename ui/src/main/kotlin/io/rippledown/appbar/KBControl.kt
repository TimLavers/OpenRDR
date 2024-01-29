package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.model.KBInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@Composable
@Preview
fun KBControl(handler: AppBarHandler) {
    var expanded by remember { mutableStateOf(false) }
    var createKbDialogShowing by remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf(0) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    val availableKBs = remember {  mutableStateListOf<KBInfo>() } // https://tigeroakes.com/posts/mutablestateof-list-vs-mutablestatelistof/
    val coroutineScope = rememberCoroutineScope()

    fun kbName() = if (kbInfo != null) kbInfo!!.name else ""

    LaunchedEffect(Unit) {
        while(true) {
            kbInfo = handler.api.kbInfo()
            val kbsApartFromCurrent = handler.api.kbList().filter { it != kbInfo }.sorted()
            availableKBs.clear()
            availableKBs.addAll(kbsApartFromCurrent)
            delay(5000)
        }
    }

    if (createKbDialogShowing) {
        val dialogState = rememberDialogState( size = DpSize(420.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { createKbDialogShowing = false },
            title = "Create KB",
            state = dialogState,

        ) {
            CreateKB(object : CreateKBHandler {
                override fun create(name: String) {
                    println("create...name: $name")
                    coroutineScope.launch {
                        kbInfo = handler.api.createKB(name)
                        println("Got new kbInfo: $kbInfo")
                    }
                    createKbDialogShowing = false
                }

                override fun cancel() {
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
            onClick = {expanded = true},
            modifier = Modifier.semantics {
                contentDescription = KB_CONTROL_DROPDOWN_DESCRIPTION
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = KB_CONTROL_DROPDOWN_DESCRIPTION,
                tint = colors.onPrimary
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = kbName(),
            color = colors.onPrimary,
            textAlign = TextAlign.Start,
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
                    role = Role.DropdownList
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
                        role = Role.Button
                        contentDescription = CREATE_KB_TEXT
                    }
            ) {
                Text(text = CREATE_KB_TEXT)
            }
            availableKBs.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    selectedIndex.value = index
                    expanded = false
                }) {
                    Text(text = option.name, modifier = Modifier.testTag(kbItemId(option)))
                }
            }
        }
    }
}
