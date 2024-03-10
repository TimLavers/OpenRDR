package io.rippledown.dragdrop

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope

// See credits.md
fun Modifier.dragGestureHandler(
    scope: CoroutineScope,
    itemListDragAndDropState: DragDropListState
): Modifier =
    this.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                itemListDragAndDropState.onDrag(offset)
            }, onDragStart = { offset -> itemListDragAndDropState.onDragStart(offset) },
            onDragEnd = { itemListDragAndDropState.onDragInterrupted() },
            onDragCancel = { itemListDragAndDropState.onDragInterrupted() })
    }