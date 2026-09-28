@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Minimal long-press drag-to-reorder for a [androidx.compose.foundation.lazy.LazyColumn]
 * whose reorderable items share one [keyPrefix]-free index space. [onMove] is
 * called every time the dragged item passes over a neighbour.
 */
class DragDropState(private val listState: LazyListState) {
    /** Called every time the dragged item passes over a neighbour. */
    var onMove: (from: Int, to: Int) -> Unit = { _, _ -> }
    /** Converts a lazy list index into a reorderable index, or null for headers/footers. */
    var toItemIndex: (Int) -> Int? = { it }

    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingOffset by mutableFloatStateOf(0f)
        private set
    private var draggingInitialOffset = 0
    private var draggingInitialSize = 0

    private fun listInfoFor(itemIndex: Int): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { toItemIndex(it.index) == itemIndex }

    fun onDragStart(itemIndex: Int) {
        val info = listInfoFor(itemIndex) ?: return
        draggingItemIndex = itemIndex
        draggingInitialOffset = info.offset
        draggingInitialSize = info.size
        draggingOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        val current = draggingItemIndex ?: return
        draggingOffset += deltaY
        val info = listInfoFor(current) ?: return
        val startOffset = info.offset + draggingOffset
        val endOffset = startOffset + info.size
        val middle = startOffset + info.size / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { other ->
            val otherIndex = toItemIndex(other.index)
            otherIndex != null && otherIndex != current &&
                middle in other.offset.toFloat()..(other.offset + other.size).toFloat()
        } ?: return
        val targetIndex = toItemIndex(target.index) ?: return
        // Keep the dragged tile under the finger when its neighbour has a different height.
        val shift = if (targetIndex > current) target.offset + target.size - endOffset else target.offset - startOffset
        onMove(current, targetIndex)
        draggingItemIndex = targetIndex
        draggingOffset -= shift
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingOffset = 0f
    }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    toItemIndex: (Int) -> Int?,
    onMove: (Int, Int) -> Unit,
): DragDropState {
    val state = remember(listState) { DragDropState(listState) }
    state.onMove = onMove
    state.toItemIndex = toItemIndex
    return state
}

/** Long-press to start dragging the item at [itemIndex]. */
fun Modifier.dragHandle(state: DragDropState, itemIndex: Int): Modifier = pointerInput(itemIndex) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.onDragStart(itemIndex) },
        onDrag = { change, offset -> change.consume(); state.onDrag(offset.y) },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
    )
}

/** Lifts and translates the item currently being dragged. */
fun Modifier.draggedItem(state: DragDropState, itemIndex: Int): Modifier {
    if (state.draggingItemIndex != itemIndex) return this
    return this
        .zIndex(1f)
        .graphicsLayer {
            translationY = state.draggingOffset
            scaleX = 1.02f
            scaleY = 1.02f
            shadowElevation = 12f
        }
}
