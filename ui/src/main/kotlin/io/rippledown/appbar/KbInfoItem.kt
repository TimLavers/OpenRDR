package io.rippledown.appbar

import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.rippledown.model.KBInfo

interface KbSelectionHandler {
    fun kbInfo(): KBInfo
    fun select()
}

@Composable
fun KbInfoItem(handler: KbSelectionHandler) {

    DropdownMenuItem(
        onClick = {
            handler.select()
        }
    ) {
        Text(text = handler.kbInfo().name)
    }
}