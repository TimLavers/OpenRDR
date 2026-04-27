package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.rippledown.constants.caseview.UNITS_HEADER_CELL_DESCRIPTION

@Composable
fun RowScope.UnitsHeaderCell(columnWidths: ColumnWidths) {
    Text(
        text = "",
        modifier = Modifier.weight(columnWidths.unitsColumnWeight)
            .semantics {
                contentDescription = UNITS_HEADER_CELL_DESCRIPTION
            }
    )
}
