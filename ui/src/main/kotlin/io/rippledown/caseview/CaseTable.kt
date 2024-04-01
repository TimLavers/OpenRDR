package io.rippledown.caseview

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.dragdrop.rememberDragDropListState
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.dragdrop.DragDropListState
import io.rippledown.dragdrop.move
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase

@Composable
fun CaseTable(viewableCase: ViewableCase, attributeMoveListener: (Attribute, Attribute) -> Unit) {
    val columnWidths = ColumnWidths(viewableCase.numberOfColumns)
    val attributes  = remember{mutableStateListOf<Attribute>()}
    attributes.clear()
    attributes.addAll(viewableCase.attributes())
    var draggedAttribute: Attribute? = null
    var targetAttribute: Attribute? = null
    val dragDropListState = rememberDragDropListState(
        onDragStarted = {
            draggedAttribute = attributes[it]
            targetAttribute = null
        },
        onMove = {
            a: Int, b: Int ->
            targetAttribute = attributes[b]
            attributes.move(a, b)
        },
        onDragFinished = {
            if (draggedAttribute != null && targetAttribute != null) {
                attributeMoveListener(draggedAttribute!!, targetAttribute!!)
            }
            draggedAttribute = null
            targetAttribute = null
        }
    )
    Column(modifier = Modifier.semantics {
        contentDescription = CASE_VIEW_TABLE
    }) {
        HeaderRow(columnWidths, viewableCase.dates)
        CaseDataTable(columnWidths, dragDropListState, attributes, viewableCase.case)
    }
}

@Composable
fun CaseDataTable(columnWidths: ColumnWidths,
                  dragDropListState: DragDropListState,
                  attributes: List<Attribute>,
                  case: RDRCase) {
    LazyColumn(
        modifier = Modifier
            .padding(5.dp)
            .pointerInput(Unit) {
                // See credits.md
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragDropListState.onDragStart(offset) },
                    onVerticalDrag = { change, offset ->
                        change.consume()
                        dragDropListState.onDrag(offset)
                    },
                    onDragCancel = { dragDropListState.onDragInterrupted() },
                    onDragEnd = { dragDropListState.onDragInterrupted() }
                )
            },
        state = dragDropListState.getLazyListState()
    ) {
        itemsIndexed(attributes) { index: Int, attribute: Attribute ->
            val resultsList = case.resultsFor(attribute)!!
            val displacementOffset = if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                dragDropListState.elementDisplacement.takeIf { it != 0f }
            } else {
                null
            }
            BodyRow(index, case.name, attribute, columnWidths, resultsList, displacementOffset)
        }
    }
}