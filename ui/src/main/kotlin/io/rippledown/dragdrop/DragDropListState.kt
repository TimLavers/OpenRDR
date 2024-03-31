package io.rippledown.dragdrop

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

// See credits.md
@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onDragStarted: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragFinished: (Int) -> Unit,
): DragDropListState {
    return remember { DragDropListState(lazyListState, onDragStarted, onMove, onDragFinished) }
}

class DragDropListState(
    private val lazyListState: LazyListState,
    private val onDragStarted: (Int) -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onDragFinished: (Int) -> Unit
) {
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    private var draggedDistance by mutableFloatStateOf(0f)
    private var currentIndexOfDraggedItem by mutableIntStateOf(-1)

    // Retrieve the currently dragged element's info
    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem.let {
            lazyListState.getVisibleItemInfoFor(absoluteIndex = it)
        }

    // Calculate the initial offsets of the dragged element
    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { Pair(it.offset, it.offsetEnd) }

    // Calculate the displacement of the dragged element
    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem
            .let { lazyListState.getVisibleItemInfoFor(absoluteIndex = it) }
            ?.let { item ->
                (initiallyDraggedElement?.offset ?: 0f).toFloat() + draggedDistance - item.offset
            }

    fun getLazyListState(): LazyListState {
        return lazyListState
    }

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
                onDragStarted(initiallyDraggedElement!!.index)
            }
    }

    // Helper function to calculate start and end offsets
    // Calculate the start and end offsets of the dragged element
    private fun calculateOffsets(offset: Float): Pair<Float, Float> {
        val startOffset = offset + draggedDistance
        val currentElementSize = currentElement?.size ?: 0
        val endOffset = offset + draggedDistance + currentElementSize
        return startOffset to endOffset
    }

    fun onDrag(offset: Float) {
        draggedDistance += offset
        val topOffset = initialOffsets?.first ?: return
        val (startOffset, endOffset) = calculateOffsets(topOffset.toFloat())

        val hoveredElement = currentElement
        if (hoveredElement != null) {
            val delta = startOffset - hoveredElement.offset
            val isDeltaPositive = delta > 0
            val isEndOffsetGreater = endOffset > hoveredElement.offsetEnd

            val validItems = lazyListState.layoutInfo.visibleItemsInfo.filter { item ->
                !(item.offsetEnd < startOffset || item.offset > endOffset || hoveredElement.index == item.index)
            }

            val targetItem = validItems.firstOrNull {
                when {
                    isDeltaPositive -> isEndOffsetGreater
                    else -> startOffset < it.offset
                }
            }

            if (targetItem != null) {
                currentIndexOfDraggedItem.let { current ->
                    onMove.invoke(current, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                }
            }
        }
    }

    fun onDragInterrupted() {
        onDragFinished( currentIndexOfDraggedItem)
        draggedDistance = 0f
        currentIndexOfDraggedItem = -1
        initiallyDraggedElement = null
    }

    fun getCurrentIndexOfDraggedListItem(): Int {
        return currentIndexOfDraggedItem
    }
}
/*
  Bottom offset of the element in Vertical list
*/
val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
    return this.layoutInfo.visibleItemsInfo.getOrNull(
        absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index
    )
}
