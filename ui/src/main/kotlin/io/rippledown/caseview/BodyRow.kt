package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
) {
    Row(
        modifier = Modifier.padding(2.dp)
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        AttributeCell(index, caseName, attribute, columnWidths)
        results.forEachIndexed { columnIndex: Int, Result: Result ->
            ValueCell(caseName, attribute, columnIndex, Result, columnWidths)
        }
        ReferenceRangeCell(attribute, results.last(), columnWidths.referenceRangeColumnWeight)
    }
}