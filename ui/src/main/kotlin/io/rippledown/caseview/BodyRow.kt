package io.rippledown.caseview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult

@Composable
fun LazyItemScope.BodyRow(
    attribute: Attribute,
    columnWidths: ColumnWidths,
    case: RDRCase
) {
    Row {
        AttributeCell(attribute, columnWidths.attributeColumnWeight)
        case.resultsFor(attribute)!!.forEachIndexed { columnIndex: Int, testResult: TestResult ->
            ValueCell(testResult, columnWidths.columnWeight(columnIndex + 1))
        }
    }
}