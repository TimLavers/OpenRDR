package io.rippledown.appbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

interface KbSelectionHandler {
    var onSelect: () -> Unit
}

const val KB_INFO_ITEM = "KB_INFO_ITEM"

@Composable
fun KbInfoItem(name: String, handler: KbSelectionHandler) {

    DropdownMenuItem(
        onClick = { handler.onSelect() },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier
            .height(28.dp)
            .semantics { contentDescription = "$KB_INFO_ITEM$name" }
    ) {
        Text(text = name, style = MaterialTheme.typography.body2)
    }
}