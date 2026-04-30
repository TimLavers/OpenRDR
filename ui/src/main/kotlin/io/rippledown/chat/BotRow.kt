package io.rippledown.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A single bot message in the chat panel.
 *
 * The enclosing Surface exposes a merged accessibility node whose
 * `contentDescription` encodes both the row identifier and the visible
 * text as `"$BOT$index:$text"`. Integration tests (see
 * `ChatPO.mostRecentBotRowContainsTerms` in the cucumber module) rely on
 * being able to substring-match against that description without walking
 * the Compose semantics subtree, which in a desktop window containing a
 * large case table can be prohibitively slow (reading `accessibleName`
 * on Compose semantics nodes can force a full-frame layout pass).
 *
 * The description therefore follows this shape:
 *
 *   "$BOT$index:<the bot's visible text>"
 *
 * Keep the colon separator in sync with `ChatPO.botRowText` and with the
 * tests in `ChatProxy.requireChatMessagesShowing`.
 */
@Composable
fun BotRow(
    text: String,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = White,
            shadowElevation = 1.dp,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = "$BOT${index}:$text"
                }
        ) {
            Text(
                text = text,
                color = Black,
                style = TextStyle(fontSize = 13.sp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
