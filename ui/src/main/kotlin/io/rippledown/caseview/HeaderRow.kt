package io.rippledown.caseview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HeaderRow(
    columnWidths: ColumnWidths,
    dates: List<Long>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    hScrollState: ScrollState = rememberScrollState(),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AttributesHeaderCell(columnWidths)
        // Date cells live in the horizontally scrollable area. The reference
        // range and units header labels stay fixed on the right.
        BoxWithConstraints(modifier = Modifier.weight(columnWidths.scrollableAreaWeight())) {
            val episodeBlockDp = maxWidth
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(hScrollState)) {
                Row(modifier = Modifier.width(episodeBlockDp * dates.size)) {
                    dates.forEachIndexed { i, date ->
                        DateCell(i, date, columnWidths)
                    }
                }
            }
        }
        ReferenceRangesHeaderCell(columnWidths)
        UnitsHeaderCell(columnWidths)
    }
}