package io.rippledown.dragdrop

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import io.rippledown.pomodoro.getVisibleItemInfoFor
import io.rippledown.pomodoro.offsetEnd

// See credits.md
@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragDropListState {
    return remember { DragDropListState(lazyListState, onMove) }
}


class DragDropListState(
    private val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
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
        println("Drag started. Offset: $offset")
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
            }
        printState()
    }

    fun printState() {
//        println("DDLS. iDE index: ${initiallyDraggedElement?.index}, cIODI: $currentIndexOfDraggedItem,dD: $draggedDistance ")
    }
    // Helper function to calculate start and end offsets
    // Calculate the start and end offsets of the dragged element
    private fun calculateOffsets(offset: Float): Pair<Float, Float> {
        val startOffset = offset + draggedDistance
        val currentElementSize = currentElement?.size ?: 0
        val endOffset = offset + draggedDistance + currentElementSize
        return startOffset to endOffset
    }

    fun onDrag(offset: Offset) {
//        println("On drag. Offset: $offset")
        draggedDistance += offset.y
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

//            println("target item: $targetItem")
            if (targetItem != null) {
                currentIndexOfDraggedItem.let { current ->
                    onMove.invoke(current, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                }
            }
        }
        printState()
    }

    fun onDragInterrupted() {
//        println("Drag interrupted")
        draggedDistance = 0f
        currentIndexOfDraggedItem = -1
        initiallyDraggedElement = null
        printState()
    }

    fun getCurrentIndexOfDraggedListItem(): Int {
        return currentIndexOfDraggedItem
    }
}