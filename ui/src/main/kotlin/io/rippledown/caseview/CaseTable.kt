package io.rippledown.caseview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

interface CaseTableHandler {
    val viewableCase: ViewableCase
}

// See
@Composable
fun CaseTable(handler: CaseTableHandler) {
    val dates = handler.viewableCase.dates
    val case = handler.viewableCase.case
    val numberOfDates = dates.size
    val columnWidths = ColumnWidths(numberOfDates)

    LazyColumn(modifier = Modifier.padding(5.dp)) {
        item {
            HeaderRow(columnWidths, dates)
        }
        itemsIndexed(handler.viewableCase.attributes()) { index: Int, attribute: Attribute ->
            BodyRow(index, attribute, columnWidths, case)
        }
    }
}