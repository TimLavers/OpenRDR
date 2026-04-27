package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rippledown.model.Attribute
import io.rippledown.model.Result

@Composable
fun BodyRow(
    index: Int,
    caseName: String,
    attribute: Attribute,
    columnWidths: ColumnWidths,
    results: List<Result>,
    displacementOffset: Float? = null,
    modifier: Modifier = Modifier,
) {
    // The last result drives whether we collapse the trailing columns into
    // the value cell:
    //   * no reference range -> value absorbs the reference-range column
    //   * also no units      -> value additionally absorbs the units column
    // This lets long text values (e.g. comments) extend across the row
    // without wrapping prematurely.
    val lastResult = results.last()
    val hasRange = rangeText(lastResult.referenceRange).isNotEmpty()
    val hasUnits = !lastResult.units.isNullOrBlank()
    val baseValueWeight = columnWidths.valueColumnWeight()
    // Text rows (no reference range AND no units) let the value cell
    // absorb the gap, range and units columns so long comments don't wrap
    // prematurely.
    val expanded = !hasRange && !hasUnits
    val lastValueWeight = if (expanded) {
        baseValueWeight +
                columnWidths.valueRangeGapWeight +
                columnWidths.referenceRangeColumnWeight +
                columnWidths.unitsColumnWeight
    } else {
        baseValueWeight
    }
    // Numeric values look much tidier right-aligned (sitting just to the
    // left of the reference-range gap), but expanded text values should
    // remain left-aligned so they read naturally.
    val lastValueAlignment = if (expanded) TextAlign.Start else TextAlign.End
    Row(
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth(),
    ) {
        AttributeCell(index, caseName, attribute, columnWidths)
        results.forEachIndexed { columnIndex: Int, Result: Result ->
            val isLast = columnIndex == results.lastIndex
            ValueCell(
                caseName, attribute, columnIndex, Result, columnWidths,
                widthWeight = if (isLast) lastValueWeight else baseValueWeight,
                textAlign = if (isLast) lastValueAlignment else TextAlign.End
            )
        }
        if (!expanded) {
            // Visual gap between the value column and the reference range.
            Spacer(modifier = Modifier.weight(columnWidths.valueRangeGapWeight))
            // The reference-range and units cells render even when blank for
            // this particular row, to keep their columns aligned across rows.
            ReferenceRangeCell(attribute, lastResult, columnWidths.referenceRangeColumnWeight)
            UnitsCell(attribute, lastResult, columnWidths.unitsColumnWeight)
        }
    }
}