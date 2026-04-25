package io.rippledown.caseview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HeaderRow(columnWidths: ColumnWidths, dates: List<Long>) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AttributesHeaderCell(columnWidths)
        dates.forEachIndexed { i, date ->
            DateCell(i, date, columnWidths)
        }
        this.ReferenceRangesHeaderCell(columnWidths)
    }
}