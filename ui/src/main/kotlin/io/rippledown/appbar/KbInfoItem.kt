package io.rippledown.appbar

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

interface KbSelectionHandler {
    var onSelect: () -> Unit
}

const val KB_INFO_ITEM = "KB_INFO_ITEM"

@Composable
fun KbInfoItem(name: String, handler: KbSelectionHandler) {

    DropdownMenuItem(
        onClick = {
            handler.onSelect()
        },
        modifier = Modifier.semantics { contentDescription = "$KB_INFO_ITEM$name" }

    ) {
        Text(text = name)
    }
}