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
    filter: String = "",
    attributeMoveListener: (Attribute, Attribute) -> Unit = { _, _ -> },
) {
    val attributes = remember(viewableCase) {
        mutableStateListOf<Attribute>().apply { addAll(viewableCase.attributes()) }
    }
    // Filter the displayed attribute list without touching the underlying
    // drag-drop source-of-truth list above. Filtering is purely a view
    // operation; reordering must always apply to the full attribute set.
    val filterActive = filter.isNotBlank()
    val displayed = if (filterActive) {
        attributes.filter { matchesFilter(viewableCase, it, filter) }
    } else {
        attributes.toList()
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

    // Drag-and-drop is suppressed while a filter is active: reordering a
    // filtered subset is ambiguous (the unseen rows still have positions),
    // so we render plain non-draggable rows and skip pointer wiring entirely.
    val dragModifier = if (filterActive) Modifier else Modifier
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
    Column(
        modifier = modifier
            .padding(5.dp)
            .semantics { contentDescription = CASE_VIEW_TABLE }
            .then(dragModifier)
    ) {
        displayed.forEachIndexed { renderIndex, attribute ->
            val resultsList = viewableCase.case.resultsFor(attribute)!!
            // Drag-drop bookkeeping is keyed on the full attribute list's
            // index; when filtered we don't register any row bounds so drag
            // becomes a no-op even if pointer events arrive.
            val fullIndex = attributes.indexOf(attribute)
            val displacementOffset = if (filterActive) 0f else dragDropState.elementDisplacementFor(fullIndex)
            BodyRow(
                index = renderIndex,
                caseName = viewableCase.name,
                attribute = attribute,
                columnWidths = columnWidths,
                results = resultsList,
                displacementOffset = displacementOffset,
                hScrollState = hScrollState,
                modifier = if (filterActive) Modifier else Modifier.onGloballyPositioned { coords ->
                    dragDropState.reportRowBounds(
                        index = fullIndex,
                        top = coords.positionInParent().y,
                        height = coords.size.height.toFloat()
                    )
                }
            )
        }
    }
}
