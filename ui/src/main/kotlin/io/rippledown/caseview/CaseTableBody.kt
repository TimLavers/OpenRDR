package io.rippledown.caseview

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import io.rippledown.dragdrop.move
import io.rippledown.dragdrop.rememberDragDropState
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
    modifier: Modifier = Modifier,
    attributeMoveListener: (Attribute, Attribute) -> Unit = { _, _ -> }
) {
    val attributes = remember { mutableStateListOf<Attribute>() }
    attributes.clear()
    attributes.addAll(viewableCase.attributes())
    var draggedAttribute: Attribute? = null
    var targetAttribute: Attribute? = null
    val dragDropState = rememberDragDropState(
        onDragStarted = {
            draggedAttribute = attributes[it]
            targetAttribute = null
        },
        onMove = { a, b ->
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
    LaunchedEffect(attributes.size) { dragDropState.ensureCapacity(attributes.size) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
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
