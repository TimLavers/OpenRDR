package io.rippledown.caseview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult

@Composable
fun BodyRow(
    index: Int,
    attribute: Attribute,
    columnWidths: ColumnWidths,
    results: List<TestResult>,
    displacementOffset: Float? = null,
) {
//    println("Body row, displacement: $displacementOffset")
    val bg = if (displacementOffset != null) Color.Green else Color.White
    Column(
        modifier = Modifier
            .graphicsLayer { translationY = displacementOffset ?: 0f }
            .background(bg, shape = RoundedCornerShape(4.dp))
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row {
                AttributeCell(index, attribute, columnWidths)
                results.forEachIndexed { columnIndex: Int, testResult: TestResult ->
                    ValueCell(attribute, columnIndex, testResult, columnWidths)
                }
                ReferenceRangeCell(attribute, results.last(), columnWidths.referenceRangeColumnWeight)
            }
        }
    }
}