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

    // Pre-compute the numbered suggestion text that's actually rendered
    // below, and fold it into this row's accessibility description. This
    // lets cucumber polling substring-match terms (e.g. "1.", "reason")
    // off a single description read rather than recursing into the row's
    // children — see [BotRow] for the performance rationale.
    val encodedSuggestions = suggestions.mapIndexed { i, suggestion ->
        val displayText = suggestion.removeSuffix(EDITABLE_MARKER)
        "${i + 1}. $displayText"
    }.joinToString("\n")

    // ~10 single-line rows fit within MAX_PANEL_HEIGHT; beyond that the panel
    // becomes scrollable and capped. The scroll/cap machinery is applied
    // ONLY when needed because `verticalScroll(...) + heightIn(max = ...) +
    // VerticalScrollbar(fillMaxHeight)` keeps the panel at the full cap
    // height even when the content is short — i.e. leaves blank space below
    // a short list. Skipping the machinery for short lists lets the panel
    // wrap to content cleanly.
    val needsScroll = suggestions.size > ROWS_BEFORE_SCROLL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$BOT${index}" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (needsScroll) Modifier.heightIn(max = MAX_PANEL_HEIGHT) else Modifier)
                .background(White, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "$SUGGESTION_LIST$index:$encodedSuggestions" }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (needsScroll) Modifier.verticalScroll(scrollState) else Modifier)
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
            if (needsScroll) {
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState)
                )
            }
        }
    }
}

private const val ROWS_BEFORE_SCROLL = 10
private val MAX_PANEL_HEIGHT = 180.dp
