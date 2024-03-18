package io.rippledown.caseview

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase


@Composable
fun CaseTable(viewableCase: ViewableCase) {
    val dates = viewableCase.dates
    val case = viewableCase.case
    val numberOfDates = dates.size
    val columnWidths = ColumnWidths(numberOfDates)

    LazyColumn(
        modifier = Modifier.padding(5.dp).semantics {
            contentDescription= CASE_VIEW_TABLE
        }
    ) {
        item {
            HeaderRow(columnWidths, dates)
        }
        itemsIndexed(viewableCase.attributes()) { index: Int, attribute: Attribute ->
            val resultsList = case.resultsFor(attribute)!!
            BodyRow(index, viewableCase.name, attribute, columnWidths, resultsList)
        }
    }
}