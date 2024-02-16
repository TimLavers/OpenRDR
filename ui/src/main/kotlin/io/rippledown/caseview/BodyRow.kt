package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult

@Composable
fun LazyItemScope.BodyRow(
    index: Int,
    attribute: Attribute,
    columnWidths: ColumnWidths,
    case: RDRCase
) {
    Row {
        AttributeCell(index, attribute, columnWidths.attributeColumnWeight)
        val resultsList = case.resultsFor(attribute)!!
        resultsList.forEachIndexed { columnIndex: Int, testResult: TestResult ->
            ValueCell(attribute, columnIndex, testResult, columnWidths.valueColumnWeight())
        }
        ReferenceRangeCell(attribute, resultsList[0], columnWidths.referenceRangeColumnWeight)
    }
}