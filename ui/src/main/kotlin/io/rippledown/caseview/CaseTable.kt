package io.rippledown.caseview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.dragdrop.dragGestureHandler
import io.rippledown.dragdrop.rememberDragDropListState
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.dragdrop.DragDropListState
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase

@Composable
fun CaseTable(viewableCase: ViewableCase) {
    val columnWidths = ColumnWidths(viewableCase.numberOfColumns)
    val attributes  = remember{mutableStateListOf<Attribute>()}
    attributes.clear()
    attributes.addAll(viewableCase.attributes())
    val dragDropListState = rememberDragDropListState(
        onMove = {
            a: Int, b: Int ->
            println("{{{{{[moving $a to $b ]}}}}}")
            attributes.move(a, b)
        }
    )
    Column {
        HeaderRow(columnWidths, viewableCase.dates)
        CaseDataTable(columnWidths, dragDropListState, attributes, viewableCase.case)
    }
}

@Composable
fun CaseDataTable(columnWidths: ColumnWidths,
                  dragDropListState: DragDropListState,
                  attributes: List<Attribute>,
                  case: RDRCase) {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.padding(5.dp).semantics {
            contentDescription = CASE_VIEW_TABLE
        }.dragGestureHandler(coroutineScope, dragDropListState),
        state = dragDropListState.getLazyListState()
    ) {
        println("attributes: ${attributes.size}")
        itemsIndexed(attributes) { index: Int, attribute: Attribute ->
            val resultsList = case.resultsFor(attribute)!!
            val displacementOffset = if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                dragDropListState.elementDisplacement.takeIf { it != 0f }
            } else {
                null
            }
            BodyRow(index, viewableCase.name, attribute, columnWidths, resultsList, displacementOffset)
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