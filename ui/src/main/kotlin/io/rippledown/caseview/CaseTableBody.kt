package io.rippledown.caseview

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.dragdrop.DragDropState
import io.rippledown.dragdrop.move
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase

/**
 * Renders just the attribute rows of a case (no dates header). Intended to be
 * placed inside a scrollable container while the case name and dates header
 * remain fixed above it.
 */
@Composable
fun CaseTableBody(
    viewableCase: ViewableCase,
    columnWidths: ColumnWidths,
    modifier: Modifier = Modifier.fillMaxWidth(),
    hScrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
    attributeMoveListener: (Attribute, Attribute) -> Unit = { _, _ -> },
) {
    val attributes = remember(viewableCase) {
        mutableStateListOf<Attribute>().apply { addAll(viewableCase.attributes()) }
    }
    var draggedAttribute: Attribute? = null
    var targetAttribute: Attribute? = null
    // The drag-drop state is keyed on `viewableCase` so that selecting a
    // different case (which may have a different number of attributes)
    // discards any stale row-bounds tracking from the previous case. Without
    // this, an in-flight pointer event arriving after the case swap could be
    // hit-tested against rows that no longer exist and index past the end of
    // `attributes`.
    val dragDropState = remember(viewableCase) {
        DragDropState(
            onDragStarted = {
                if (it in attributes.indices) {
                    draggedAttribute = attributes[it]
                    targetAttribute = null
                }
            },
            onMove = { a, b ->
                if (b in attributes.indices && a in attributes.indices) {
                    targetAttribute = attributes[b]
                    attributes.move(a, b)
                }
            },
            onDragFinished = {
                if (draggedAttribute != null && targetAttribute != null) {
                    attributeMoveListener(draggedAttribute!!, targetAttribute!!)
                }
                draggedAttribute = null
                targetAttribute = null
            }
        )
    }
    dragDropState.ensureCapacity(attributes.size)

    Column(
        modifier = modifier
            .padding(5.dp)
            .semantics { contentDescription = CASE_VIEW_TABLE }
            .pointerInput(Unit) {
                // See credits.md
                detectVerticalDragGestures(
                    onDragStart = { offset -> dragDropState.onDragStart(offset) },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.onDrag(dragAmount)
                    },
                    onDragCancel = { dragDropState.onDragInterrupted() },
                    onDragEnd = { dragDropState.onDragInterrupted() }
                )
            }
    ) {
        attributes.forEachIndexed { index, attribute ->
            val resultsList = viewableCase.case.resultsFor(attribute)!!
            val displacementOffset = dragDropState.elementDisplacementFor(index)
            BodyRow(
                index = index,
                caseName = viewableCase.name,
                attribute = attribute,
                columnWidths = columnWidths,
                results = resultsList,
                displacementOffset = displacementOffset,
                hScrollState = hScrollState,
                modifier = Modifier.onGloballyPositioned { coords ->
                    dragDropState.reportRowBounds(
                        index = index,
                        top = coords.positionInParent().y,
                        height = coords.size.height.toFloat()
                    )
                }
            )
        }
    }
}
