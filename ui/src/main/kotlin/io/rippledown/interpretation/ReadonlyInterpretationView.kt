package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.rippledown.caseview.ColumnWidths
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.constants.interpretation.UNRESOLVED_VARIABLE_TOOLTIP
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation

interface ReadonlyInterpretationViewHandler {
    fun onTextLayoutResult(layoutResult: TextLayoutResult) {}
}

/**
 * The comments of an interpretation, as a two column table of the name of the
 * comment attribute that gave each comment and the comment itself. See
 * [CommentRow] for how a row previews a pending change.
 *
 * @param idPrefix distinguishes these rows from those of another Comments table
 *   on screen at the same time, as the cornerstone view's is.
 */
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun ReadonlyInterpretationView(
    interpretation: ViewableInterpretation,
    diff: Diff? = null,
    ruleConditions: List<String> = emptyList(),
    contentDescription: String = INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE,
    columnWidths: ColumnWidths = ColumnWidths(1),
    idPrefix: String = "",
    modifier: Modifier,
    handler: ReadonlyInterpretationViewHandler
) {
    val rows = commentRowsToDisplay(interpretation.renderedComments, diff, ruleConditions)
    Column(
        modifier = modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription }
    ) {
        rows.forEach { row ->
            CommentRow(row = row, columnWidths = columnWidths, idPrefix = idPrefix)
        }
    }
}

@Composable
fun UnresolvedVariableTooltip() {
    androidx.compose.material3.Surface(
        color = UNRESOLVED_COLOR,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        shadowElevation = 4.dp
    ) {
        androidx.compose.material.Text(
            text = UNRESOLVED_VARIABLE_TOOLTIP,
            modifier = Modifier
                .padding(8.dp)
                .semantics { contentDescription = UNRESOLVED_VARIABLE_TOOLTIP }
        )
    }
}