package io.rippledown.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * @author Cascade AI
 */
@Composable
fun SuggestionListRow(
    suggestions: List<String>,
    index: Int,
    onSuggestionClicked: (String, Boolean) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .semantics { contentDescription = "$BOT${index}" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 80.dp)
                .background(White, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "$SUGGESTION_LIST$index" }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                suggestions.forEachIndexed { i, suggestion ->
                    val isEditable = suggestion.endsWith(EDITABLE_MARKER)
                    val displayText = if (isEditable) suggestion.removeSuffix(EDITABLE_MARKER) else suggestion
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { onSuggestionClicked(displayText, isEditable) }
                            .semantics { contentDescription = "$SUGGESTION_ITEM$displayText" }
                    ) {
                        Text(
                            text = "${i + 1}. $displayText",
                            color = Black,
                            style = TextStyle(fontSize = 13.sp)
                        )
                        if (isEditable) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "editable",
                                tint = Gray,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}
