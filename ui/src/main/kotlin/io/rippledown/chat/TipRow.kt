package io.rippledown.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
 * A single, gently highlighted tip in the chat panel - distinct from an ordinary
 * [BotRow] so that an occasional, informational hint (e.g. "you can include a case
 * value in a comment by wrapping an attribute name in braces") stands out without
 * looking like a warning or error.
 *
 * Like [BotRow], the enclosing Surface exposes a merged accessibility node whose
 * `contentDescription` encodes the row identifier and the visible text as
 * `"$TIP$index:$text"`. Integration tests prefer the [ChatTestHook] tip channel,
 * but the description keeps the row substring-findable for parity with the other rows.
 */
@Composable
fun TipRow(
    text: String,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TIP_BACKGROUND,
            shadowElevation = 1.dp,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "$TIP${index}:$text"
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = TIP_ACCENT,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = TIP_TEXT,
                    style = TextStyle(fontSize = 13.sp),
                )
            }
        }
    }
}

private val TIP_BACKGROUND = Color(0xFFFFF8E1) // soft amber
private val TIP_ACCENT = Color(0xFFF9A825) // amber 800
private val TIP_TEXT = Color(0xFF5D4037) // warm brown-grey for readable contrast
