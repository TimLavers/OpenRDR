@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rippledown.model.RenderedComment

/**
 * A name chip and the comment beside it, tooltipped with the conditions of the
 * rule that gave the comment.
 *
 * A whole row is one part; a row previewing a replacement is two, the comment
 * going and the comment coming in its place, each with its own name, so that a
 * name is never separated from its text.
 *
 * Hovering over the comment reports the hover to [onHoverChanged], so that the
 * row can highlight itself, and shows the conditions of the rule that gave the
 * comment. Hovering over an unresolved variable marker within the comment
 * explains that instead.
 *
 * @param partBackground tints this part alone, as against the whole row, which
 *   is how the two halves of a replacement are tinted differently.
 * @param nameWeight the share of this part taken by the name chip.
 */
@Composable
internal fun RowScope.CommentPart(
    comment: RenderedComment,
    partBackground: Color,
    textDescription: String,
    nameDescription: String,
    nameWeight: Float,
    partWeight: Float,
    onHoverChanged: (Boolean) -> Unit
) {
    var isOverUnresolved by remember { mutableStateOf(false) }
    TooltipArea(
        tooltip = {
            if (isOverUnresolved) {
                UnresolvedVariableTooltip()
            } else {
                ConditionTooltip(comment.conditions)
            }
        },
        modifier = Modifier.weight(partWeight).background(partBackground)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CommentNameChip(
                name = comment.name,
                description = nameDescription,
                modifier = Modifier.weight(nameWeight).padding(end = 8.dp)
            )
            AnnotatedTextView(
                text = comment.annotatedText(),
                description = textDescription,
                modifier = Modifier.weight(1f - nameWeight).padding(vertical = 2.dp),
                style = TextStyle(fontSize = 13.sp, color = Color.Black),
                handler = object : AnnotatedTextViewHandler {
                    override fun onTextLayoutResult(layoutResult: TextLayoutResult) {}

                    override fun onPointerEnter(characterOffset: Int) {
                        onHoverChanged(true)
                        isOverUnresolved = comment.unresolvedRanges.any { characterOffset in it.toIntRange() }
                    }

                    override fun onPointerExit() {
                        onHoverChanged(false)
                        isOverUnresolved = false
                    }
                }
            )
        }
    }
}

/**
 * The name of a comment attribute, as a chip, so that it reads as a label of
 * the comment beside it rather than as part of the comment's text.
 */
@Composable
private fun CommentNameChip(name: String, description: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.LightGray.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                .semantics { contentDescription = description }
        )
    }
}

/**
 * The comment's text, with any unresolved variable markers highlighted.
 */
internal fun RenderedComment.annotatedText() = buildAnnotatedString {
    append(text)
    unresolvedRanges.forEach { range ->
        addStyle(SpanStyle(background = UNRESOLVED_COLOR), range.start, range.endInclusive + 1)
    }
}
