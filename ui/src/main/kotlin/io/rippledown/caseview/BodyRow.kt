package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
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
    val lastValueWeight = baseValueWeight +
            (if (hasRange) 0F else columnWidths.referenceRangeColumnWeight) +
            (if (hasRange || hasUnits) 0F else columnWidths.unitsColumnWeight)
    // Numeric values look much tidier right-aligned (sitting next to their
    // reference range), but text values that have been allowed to expand
    // across the trailing columns should remain left-aligned so they read
    // naturally.
    val expanded = !hasRange && !hasUnits
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
        if (hasRange) {
            ReferenceRangeCell(attribute, lastResult, columnWidths.referenceRangeColumnWeight)
        }
        // Render the units cell whenever there is a reference range OR units
        // to display, to keep the reference-range column aligned across rows.
        // Only collapse it away when the value cell has already absorbed it.
        if (hasRange || hasUnits) {
            UnitsCell(attribute, lastResult, columnWidths.unitsColumnWeight)
        }
    }
}