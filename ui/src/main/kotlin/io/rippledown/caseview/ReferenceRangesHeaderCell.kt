package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.rippledown.constants.caseview.REFERENCE_RANGE_HEADER_CELL_DESCRIPTION

@Composable
fun RowScope.ReferenceRangesHeaderCell(columnWidths: ColumnWidths) {
    Text(
        text = "",
        modifier = Modifier.weight(columnWidths.referenceRangeColumnWeight)
            .semantics {
                contentDescription = REFERENCE_RANGE_HEADER_CELL_DESCRIPTION
            }
    )
}
