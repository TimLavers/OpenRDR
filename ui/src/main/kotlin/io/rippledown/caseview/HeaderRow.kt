package io.rippledown.caseview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun HeaderRow(columnWidths: ColumnWidths, dates: List<Long>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AttributesHeaderCell(columnWidths)
        dates.forEachIndexed { i, date ->
            DateCell(i, date, columnWidths)
        }
        this.ReferenceRangesHeaderCell(columnWidths)
    }
}