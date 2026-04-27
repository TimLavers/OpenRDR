package io.rippledown.caseview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    hScrollState: ScrollState = rememberScrollState(),
) {
    // The attribute, reference range and units columns stay fixed; only the
    // dates and per-episode values scroll horizontally between them. The
    // reference range and units shown are taken from the most recent
    // (last) result for this attribute.
    val lastResult = results.last()
    val hasRange = rangeText(lastResult.referenceRange).isNotEmpty()
    val hasUnits = !lastResult.units.isNullOrBlank()
    val baseValueWeight = columnWidths.valueColumnWeight()
    val gapWeight = columnWidths.valueRangeGapWeight
    val rangeWeight = columnWidths.referenceRangeColumnWeight
    val unitsWeight = columnWidths.unitsColumnWeight
    val scrollableWeight = columnWidths.scrollableAreaWeight()
    // Text-only rows (no reference range and no units) let the value cell
    // fill the full per-episode block so long comments read naturally
    // without wrapping prematurely.
    val expanded = !hasRange && !hasUnits
    Row(
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth(),
    ) {
        AttributeCell(index, caseName, attribute, columnWidths)
        BoxWithConstraints(modifier = Modifier.weight(scrollableWeight)) {
            val episodeBlockDp = maxWidth
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(hScrollState)) {
                Row(modifier = Modifier.width(episodeBlockDp * results.size)) {
                    results.forEachIndexed { columnIndex: Int, result: Result ->
                        if (expanded) {
                            ValueCell(
                                caseName, attribute, columnIndex, result, columnWidths,
                                widthWeight = baseValueWeight + gapWeight,
                                textAlign = TextAlign.Start,
                            )
                        } else {
                            ValueCell(
                                caseName, attribute, columnIndex, result, columnWidths,
                                widthWeight = baseValueWeight,
                                textAlign = TextAlign.End,
                            )
                            Spacer(modifier = Modifier.weight(gapWeight))
                        }
                    }
                }
            }
        }
        if (expanded) {
            // No reference range / units to show for text rows; reserve the
            // same fixed slot so columns line up across rows.
            Spacer(modifier = Modifier.weight(rangeWeight + unitsWeight))
        } else {
            ReferenceRangeCell(attribute, lastResult, rangeWeight)
            UnitsCell(attribute, lastResult, unitsWeight)
        }
    }
}