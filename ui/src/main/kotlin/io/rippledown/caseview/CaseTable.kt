package io.rippledown.caseview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.dragdrop.dragGestureHandler
import io.rippledown.dragdrop.rememberDragDropListState
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
    val coroutineScope = rememberCoroutineScope()
    val attributes = mutableStateListOf<Attribute>()
    attributes.addAll(handler.viewableCase.attributes())
    val dragDropListState = rememberDragDropListState(
        onMove = {
            a: Int, b: Int ->
            println("{{{{{[moving]}}}}}")
            attributes.swap(a, b)
        }
    )

    LazyColumn(
        modifier = Modifier.padding(5.dp).semantics {
            contentDescription = "Case view table"
        }.dragGestureHandler(coroutineScope, dragDropListState),
        state = dragDropListState.getLazyListState()
    ) {
        item {
            HeaderRow(columnWidths, dates)
        }
        println("attributes: $attributes")
        itemsIndexed(attributes) { index: Int, attribute: Attribute ->
            val resultsList = case.resultsFor(attribute)!!
            val displacementOffset = if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                dragDropListState.elementDisplacement.takeIf { it != 0f }
            } else {
                null
            }
            BodyRow(index, attribute, columnWidths, resultsList, displacementOffset)
        }
    }
}

/*
   Moving element in the list
*/
fun <T> MutableList<T>.move(
    from: Int,
    to: Int
) {
    if (from == to)
        return
    val element = this.removeAt(from) ?: return
    this.add(to, element)
}

fun <T> List<T>.swap(index1: Int, index2: Int): List<T> {
    if (index1 == index2 || index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) {
        return this
    }

    val result = toMutableList()
    val temp = result[index1]
    result[index1] = result[index2]
    result[index2] = temp
    return result
}