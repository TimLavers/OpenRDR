package io.rippledown.caseview

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

/**
 * Convenience wrapper that renders the dates [HeaderRow] above the attribute
 * rows ([CaseTableBody]). Kept for tests and for callers that want the full
 * non-scrolling table as a single unit. Scrolling parents should compose
 * [HeaderRow] (fixed) and [CaseTableBody] (scrollable) separately instead.
 */
@Composable
fun CaseTable(
    viewableCase: ViewableCase,
    modifier: Modifier = Modifier,
    attributeMoveListener: (Attribute, Attribute) -> Unit = { _, _ -> }
) {
    val columnWidths = ColumnWidths(viewableCase.numberOfColumns)
    Column(modifier = modifier) {
        HeaderRow(columnWidths, viewableCase.dates)
        CaseTableBody(
            viewableCase = viewableCase,
            columnWidths = columnWidths,
            attributeMoveListener = attributeMoveListener
        )
    }
}
