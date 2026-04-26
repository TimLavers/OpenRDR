package io.rippledown.dragdrop

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

/**
 * State for reordering items in a vertical column via drag-and-drop, without
 * requiring a `LazyColumn`. Each rendered row reports its bounds via
 * [reportRowBounds] (typically from `onGloballyPositioned`), and pointer-input
 * callbacks call [onDragStart], [onDrag] and [onDragInterrupted].
 *
 * The single row that is currently being dragged should apply
 * [elementDisplacementFor] as a `graphicsLayer.translationY` so its rendering
 * follows the pointer while the underlying list is reordered live.
 */
@Composable
fun rememberDragDropState(
    onDragStarted: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragFinished: (Int) -> Unit
): DragDropState = remember { DragDropState(onDragStarted, onMove, onDragFinished) }

class DragDropState(
    private val onDragStarted: (Int) -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onDragFinished: (Int) -> Unit
) {
    /** Top-edge in pixels of each row, indexed by current attribute index. */
    private val rowTops = mutableStateListOf<Float>()

    /** Height in pixels of each row. */
    private val rowHeights = mutableStateListOf<Float>()

    private var draggedDistance by mutableStateOf(0f)
    private var currentIndexOfDraggedItem by mutableStateOf(-1)
    private var initialTopOfDraggedItem = 0f

    /**
     * Resize the bounds tracking arrays when the underlying list changes. Safe
     * to call every recomposition.
     */
    fun ensureCapacity(size: Int) {
        while (rowTops.size < size) {
            rowTops.add(0f)
            rowHeights.add(0f)
        }
        while (rowTops.size > size) {
            rowTops.removeAt(rowTops.size - 1)
            rowHeights.removeAt(rowHeights.size - 1)
        }
    }

    fun reportRowBounds(index: Int, top: Float, height: Float) {
        if (index < 0) return
        if (index >= rowTops.size) ensureCapacity(index + 1)
        rowTops[index] = top
        rowHeights[index] = height
    }

    /**
     * The visual offset to apply to the row at [index] so that, while it is
     * being dragged, it follows the cursor even after the underlying list has
     * been reordered. Returns `null` for any non-dragged row.
     */
    fun elementDisplacementFor(index: Int): Float? {
        if (currentIndexOfDraggedItem != index) return null
        if (index !in rowTops.indices) return null
        return initialTopOfDraggedItem + draggedDistance - rowTops[index]
    }

    fun onDragStart(offset: Offset) {
        val y = offset.y
        for (i in rowTops.indices) {
            val top = rowTops[i]
            val bottom = top + rowHeights[i]
            if (y in top..bottom) {
                currentIndexOfDraggedItem = i
                initialTopOfDraggedItem = top
                onDragStarted(i)
                return
            }
        }
    }

    fun onDrag(delta: Float) {
        if (currentIndexOfDraggedItem == -1) return
        draggedDistance += delta
        val current = currentIndexOfDraggedItem
        val currentHeight = rowHeights.getOrElse(current) { 0f }
        // Use the dragged row's center as the hit-test point so that a swap
        // happens only when the row has visually moved past a neighbour's
        // midpoint, rather than as soon as either edge crosses it. This
        // matches the "drop where the row's center lands" semantics expected
        // by the integration tests and avoids over-shooting through several
        // neighbours during a single drag.
        val draggedCenter = initialTopOfDraggedItem + draggedDistance + currentHeight / 2f

        val target = rowTops.indices.firstOrNull { i ->
            if (i == current) return@firstOrNull false
            val rowMid = rowTops[i] + rowHeights[i] / 2f
            if (i > current) draggedCenter >= rowMid else draggedCenter <= rowMid
        }
        if (target != null && target != current) {
            onMove(current, target)
            currentIndexOfDraggedItem = target
        }
    }

    fun onDragInterrupted() {
        onDragFinished(currentIndexOfDraggedItem)
        draggedDistance = 0f
        currentIndexOfDraggedItem = -1
    }
}
