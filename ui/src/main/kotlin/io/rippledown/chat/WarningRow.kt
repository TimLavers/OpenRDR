package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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

/**
 * A warning in the chat panel: something the user did in the GUI was not
 * carried out, e.g. an attribute reorder refused because another user is
 * editing the knowledge base. Distinct from a [BotRow] so that it reads as
 * a notice rather than a reply, and from a [TipRow] so that it is not
 * mistaken for a hint.
 *
 * As for the other rows, the Surface exposes a merged accessibility node
 * whose `contentDescription` is `"$WARNING$index:$text"`.
 */
@Composable
fun WarningRow(
    text: String,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = WARNING_BACKGROUND,
            shadowElevation = 1.dp,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "$WARNING${index}:$text"
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = WARNING_ACCENT,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = WARNING_TEXT,
                    style = TextStyle(fontSize = 13.sp),
                )
            }
        }
    }
}

private val WARNING_BACKGROUND = Color(0xFFFDECEA) // soft red
private val WARNING_ACCENT = Color(0xFFC62828) // red 800
private val WARNING_TEXT = Color(0xFF5F2120) // dark red-brown for readable contrast
