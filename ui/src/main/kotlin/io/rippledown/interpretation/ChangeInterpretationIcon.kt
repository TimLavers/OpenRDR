@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rippledown.constants.interpretation.CHANGE_INTERPRETATION
import io.rippledown.constants.interpretation.CHANGE_INTERPRETATION_ICON

typealias OnClick = () -> Unit

@Composable
fun ChangeInterpretationIcon(handler: OnClick) {
    TooltipArea(
        tooltip = {
            // Tooltip content
            Surface(
                color = Color.Gray,
                shape = MaterialTheme.shapes.small,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = CHANGE_INTERPRETATION,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        delayMillis = 300 // Delay before tooltip appears
    ) {
        // Wrapped content: the clickable icon
        IconButton(onClick = handler) {
            Icon(Icons.Filled.Edit, CHANGE_INTERPRETATION_ICON)
        }
    }
}
