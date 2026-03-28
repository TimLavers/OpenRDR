package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Surface
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
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = LIGHT_GREY,
            shadowElevation = 1.dp,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "$USER${index}"
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = displayText,
                    color = DARK_GREY,
                    style = TextStyle(fontSize = 13.sp)
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
        }
    }
}
