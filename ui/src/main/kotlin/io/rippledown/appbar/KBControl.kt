package io.rippledown.appbar

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.rippledown.constants.kb.KB_CONTROL_ID
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.CREATE_KB_ITEM_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_ID
import io.rippledown.constants.main.kbItemId
import io.rippledown.model.KBInfo

@Composable
@Preview
fun KBControl(handler: AppBarHandler) {
    var expanded by remember { mutableStateOf(false) }
    var createKbDialogShowing by remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf(0) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    val availableKBs = mutableListOf<KBInfo>()

    fun kbName() = if (kbInfo != null) kbInfo!!.name else ""

    LaunchedEffect(Unit) {
        kbInfo = handler.api.kbInfo()
        val kbsApartFromCurrent = handler.api.kbList().filter { it != kbInfo }.sorted()
        availableKBs.clear()
        availableKBs.addAll(kbsApartFromCurrent)
    }

    if (createKbDialogShowing) {
        val dialogState = rememberDialogState( size = DpSize(420.dp, 160.dp))
        DialogWindow(
            onCloseRequest = { createKbDialogShowing = false },
            title = "Create KB",
            state = dialogState
        ) {
            CreateKB(object : CreateKBHandler {
                override fun create(name: String) {
//                    kbInfo = handler.api.createKB(name)
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
            .clickable(onClick = { expanded = true })
            .background(color = colors.primary)
            .padding(16.dp)
            .testTag(KB_CONTROL_ID)

    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Select a Knowledge Base",
            tint = colors.onPrimary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = kbName(),
            color = colors.onPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(1f)
                .testTag(KB_SELECTOR_ID)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag(KBS_DROPDOWN_ID)
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    createKbDialogShowing = true
                },
                modifier = Modifier.testTag(CREATE_KB_ITEM_ID)
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
