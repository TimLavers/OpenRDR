package io.rippledown.caseview

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult

@Composable
fun BodyRow(
    index: Int,
    caseName: String,
    attribute: Attribute,
    columnWidths: ColumnWidths,
    results: List<TestResult>,
    displacementOffset: Float? = null,
) {
    Row(
        modifier = Modifier.padding(8.dp)
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        AttributeCell(index, caseName, attribute, columnWidths)
        results.forEachIndexed { columnIndex: Int, testResult: TestResult ->
            ValueCell(caseName, attribute, columnIndex, testResult, columnWidths)
        }
        ReferenceRangeCell(attribute, results.last(), columnWidths.referenceRangeColumnWeight)
    }
}