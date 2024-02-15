package io.rippledown.caseview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LazyItemScope.HeaderRow(columnWidths: ColumnWidths, dates: List<Long>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AttributesHeaderCell( columnWidths.columnWeight(0))
        dates.forEachIndexed { i, date ->
            DateCell(i, date, columnWidths.columnWeight(i + 1))
        }
    }
}