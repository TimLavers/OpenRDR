package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.SuggestionChipDefaults.elevatedSuggestionChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.decoration.DARK_GREY
import io.rippledown.decoration.LIGHT_GREY

@Composable
fun UserRow(
    text: String,
    index: Int,
) {
    val isEditable = text.endsWith(EDITABLE_MARKER)
    val displayText = if (isEditable) text.removeSuffix(EDITABLE_MARKER) else text

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        ElevatedSuggestionChip(
            onClick = { },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayText,
                        color = DARK_GREY,
                        style = TextStyle(fontSize = 14.sp)
                    )
                    if (isEditable) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "editable",
                            tint = Color.Gray,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(14.dp)
                        )
                    }
                }
            },
            colors = elevatedSuggestionChipColors(
                containerColor = LIGHT_GREY,
                labelColor = DARK_GREY
            ),
            modifier = Modifier
                .semantics {
                    contentDescription = "$USER${index}"
                }
        )
    }
}
