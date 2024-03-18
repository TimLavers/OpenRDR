package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult

@Composable
fun BodyRow(
    index: Int,
    caseName: String,
    attribute: Attribute,
    columnWidths: ColumnWidths,
    results: List<TestResult>
) {
    Row {
        AttributeCell(index, caseName, attribute, columnWidths)
        results.forEachIndexed { columnIndex: Int, testResult: TestResult ->
            ValueCell(caseName, attribute, columnIndex, testResult, columnWidths)
        }
        ReferenceRangeCell(attribute, results.last(), columnWidths.referenceRangeColumnWeight)
    }
}