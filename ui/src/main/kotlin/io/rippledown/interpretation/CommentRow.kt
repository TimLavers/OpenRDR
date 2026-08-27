@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.ColumnWidths
import io.rippledown.constants.interpretation.*
import io.rippledown.decoration.BACKGROUND_COLOR

/**
 * One row of the Comments table: the name of the comment attribute that gave
 * the comment, as a chip, and the comment itself.
 *
 * A comment being added or removed by the rule session in progress tints the
 * whole row, green or red. A replacement shows two halves side by side: the
 * comment going, in red, and the comment coming in its place, in green, each
 * with its own name, so that a name is never separated from its text.
 *
 * Hovering over a row highlights it and shows the conditions of the rule that
 * gave the comment; hovering over an unresolved variable marker within the
 * comment explains that instead.
 *
 * @param idPrefix distinguishes these rows from those of another Comments
 *   table on screen at the same time, as the cornerstone view's is.
 */
@Composable
internal fun CommentRow(
    row: CommentRowState,
    columnWidths: ColumnWidths = ColumnWidths(1),
    idPrefix: String = ""
) {
    var hovered by remember { mutableStateOf(false) }
    val isReplacement = row.highlight == CommentHighlight.REPLACED && row.replacement != null
    val rowBackground = when {
        row.highlight == CommentHighlight.ADDED -> DIFF_ADDITION_COLOR
        row.highlight == CommentHighlight.REMOVED -> DIFF_REMOVAL_COLOR
        hovered -> BACKGROUND_COLOR
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .background(rowBackground)
            .testTag(
                if (hovered) "$idPrefix$COMMENT_ROW_HOVERED_TAG_PREFIX${row.comment.name}"
                else "$idPrefix$COMMENT_ROW_TAG_PREFIX${row.comment.name}"
            )
            .semantics { contentDescription = "$idPrefix$COMMENT_ROW_PREFIX${row.comment.name}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommentPart(
            comment = row.comment,
            // The comment being replaced is tinted within its own half, so that
            // the replacement beside it can be tinted differently.
            partBackground = if (isReplacement) DIFF_REMOVAL_COLOR else Color.Transparent,
            textDescription = "$idPrefix${row.textPrefix()}${row.comment.name}",
            nameDescription = "$idPrefix$COMMENT_NAME_PREFIX${row.comment.name}",
            nameWeight = if (isReplacement) REPLACEMENT_NAME_WEIGHT else columnWidths.attributeColumnWeight,
            partWeight = if (isReplacement) 0.5f else 1f,
            onHoverChanged = { hovered = it }
        )
        if (isReplacement) {
            val replacement = row.replacement!!
            CommentPart(
                comment = replacement,
                partBackground = DIFF_ADDITION_COLOR,
                textDescription = "$idPrefix$COMMENT_REPLACEMENT_TEXT_PREFIX${replacement.name}",
                nameDescription = "$idPrefix$COMMENT_REPLACEMENT_NAME_PREFIX${replacement.name}",
                nameWeight = REPLACEMENT_NAME_WEIGHT,
                partWeight = 0.5f,
                onHoverChanged = { hovered = it }
            )
        }
    }
}

/**
 * The name column of a replacement half. The halves are narrower than a whole
 * row, so the name takes a larger share of one than it does of a full row,
 * where it lines up with the attribute column of the case table.
 */
private const val REPLACEMENT_NAME_WEIGHT = 0.25f

/**
 * The prefix of the text cell's id, encoding the pending change, if any, that
 * the row previews, so that the highlighting, which the semantics tree does not
 * show, can be asserted.
 */
private fun CommentRowState.textPrefix() = when (highlight) {
    CommentHighlight.ADDED -> COMMENT_PENDING_ADD_PREFIX
    CommentHighlight.REMOVED -> COMMENT_PENDING_REMOVE_PREFIX
    CommentHighlight.REPLACED -> COMMENT_PENDING_REPLACE_PREFIX
    CommentHighlight.NONE -> COMMENT_TEXT_PREFIX
}
