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
import androidx.compose.ui.unit.dp
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.model.KBInfo


@Composable
@Preview
fun KBControl(handler: AppBarHandler) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = remember { mutableStateOf(0) }
    var kbInfo: KBInfo? by remember { mutableStateOf(null) }
    val options = listOf("Thyroids", "Lipids", "Glucose")

    fun kbName() = if (kbInfo != null) kbInfo!!.name else ""

    LaunchedEffect(Unit) {
        kbInfo = handler.api.kbInfo()
    }

    Row(
        Modifier
            .clickable(onClick = { expanded = true })
            .background(color = colors.primary)
            .padding(16.dp)

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
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(onClick = {
                    selectedIndex.value = index
                    expanded = false
                }) {
                    Text(text = option)
                }
            }
        }
    }
}
