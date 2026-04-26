package io.rippledown.dragdrop

import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * Unit tests for [DragDropState]. The state object is pure, non-Composable
 * logic so we can exercise it directly without a Compose test rule. Each
 * test follows a Given / When / Then structure.
 *
 * The fixture below configures a list of three rows, each of height 20px,
 * stacked vertically with no gaps:
 *
 *   index 0 -> top =   0, bottom =  20
 *   index 1 -> top =  20, bottom =  40
 *   index 2 -> top =  40, bottom =  60
 */
class DragDropStateTest {

    private val rowCount = 3
    private val rowHeight = 20f

    private fun newState(
        onDragStarted: (Int) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onDragFinished: (Int) -> Unit = {}
    ): DragDropState {
        val state = DragDropState(onDragStarted, onMove, onDragFinished)
        state.ensureCapacity(rowCount)
        repeat(rowCount) { i ->
            state.reportRowBounds(index = i, top = i * rowHeight, height = rowHeight)
        }
        return state
    }

    @Test
    fun `ensureCapacity grows and shrinks the row tracking arrays`() {
        // Given a state listening for drag starts
        var started = -1
        val state = DragDropState({ started = it }, { _, _ -> }, {})

        // When growing capacity to two rows and reporting their bounds
        state.ensureCapacity(2)
        state.reportRowBounds(0, 0f, 10f)
        state.reportRowBounds(1, 10f, 10f)

        // Then a drag inside row 1 picks up that row
        state.onDragStart(Offset(0f, 15f))
        started shouldBe 1

        // And When the capacity is shrunk to a single row
        started = -1
        state.ensureCapacity(1)

        // Then the previously-reported second row is no longer drag-startable
        state.onDragStart(Offset(0f, 15f))
        started shouldBe -1
        // But row 0 still is.
        state.onDragStart(Offset(0f, 5f))
        started shouldBe 0
    }

    @Test
    fun `reportRowBounds grows the tracking arrays on demand`() {
        // Given a state sized for two rows but with bounds reported eagerly
        var started = -1
        val state = DragDropState({ started = it }, { _, _ -> }, {})
        state.ensureCapacity(2)

        // When an index beyond the current capacity is reported
        // (e.g. `onGloballyPositioned` fires before `ensureCapacity` has been
        // called for the new row count)
        state.reportRowBounds(index = 4, top = 100f, height = 20f)

        // Then a drag inside that newly-tracked row picks it up
        state.onDragStart(Offset(0f, 110f))
        started shouldBe 4
    }

    @Test
    fun `reportRowBounds ignores negative indices`() {
        // Given a state sized for 2 rows
        var started = -1
        val state = DragDropState({ started = it }, { _, _ -> }, {})
        state.ensureCapacity(2)

        // When a negative index is reported
        state.reportRowBounds(index = -1, top = 0f, height = 20f)

        // Then no drag is started for it
        state.onDragStart(Offset(0f, 5f))
        started shouldBe -1
    }

    @Test
    fun `onDragStart picks the row whose vertical bounds contain the offset`() {
        // Given a state with three reported rows and a started-listener
        var startedIndex = -1
        val state = newState(onDragStarted = { startedIndex = it })

        // When the drag starts somewhere inside the middle row
        state.onDragStart(Offset(x = 0f, y = 25f))

        // Then the middle row index is reported as the dragged row
        startedIndex shouldBe 1
    }

    @Test
    fun `onDragStart outside any row does not start a drag`() {
        // Given a state with a started-listener
        var startedIndex = -1
        val state = newState(onDragStarted = { startedIndex = it })

        // When the drag starts below the last row
        state.onDragStart(Offset(x = 0f, y = 999f))

        // Then no row is reported
        startedIndex shouldBe -1
    }

    @Test
    fun `onDrag moves the dragged row past a downstream row once its center crosses the neighbour midpoint`() {
        // Given a drag started on row 0 (rows are y in [0,20], [20,40], [40,60])
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = newState(onMove = { from, to -> moves += from to to })
        state.onDragStart(Offset(0f, 5f)) // row 0; initial center at y=10

        // When dragging downwards far enough that row 0's center crosses row 1's midpoint at y=30
        state.onDrag(21f)

        // Then row 0 has been moved to position 1
        moves shouldBe listOf(0 to 1)
    }

    @Test
    fun `onDrag does not fire onMove when the drag stays within the dragged row`() {
        // Given a drag started on row 1
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = newState(onMove = { from, to -> moves += from to to })
        state.onDragStart(Offset(0f, 25f))

        // When dragged a tiny amount that does not cross any neighbour midpoint
        state.onDrag(2f)

        // Then no reorder is reported
        moves shouldBe emptyList()
    }

    @Test
    fun `onDrag moves the dragged row upwards past the previous row`() {
        // Given a drag started on the last row (initial center at y=50)
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = newState(onMove = { from, to -> moves += from to to })
        state.onDragStart(Offset(0f, 50f)) // row 2

        // When dragged upwards far enough that the dragged center crosses row 1's midpoint at y=30
        state.onDrag(-21f)

        // Then row 2 reorders to position 1
        moves shouldBe listOf(2 to 1)
    }

    @Test
    fun `onDrag is a no-op when no drag has been started`() {
        // Given a fresh state with no drag started
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = newState(onMove = { from, to -> moves += from to to })

        // When onDrag is called without an enclosing onDragStart
        state.onDrag(50f)

        // Then no moves are reported
        moves shouldBe emptyList()
    }

    @Test
    fun `elementDisplacementFor returns null for non-dragged rows`() {
        // Given an active drag on row 0
        val state = newState()
        state.onDragStart(Offset(0f, 5f))
        state.onDrag(5f)

        // When asking for the displacement of a non-dragged row
        val displacement = state.elementDisplacementFor(1)

        // Then the displacement is null
        displacement shouldBe null
    }

    @Test
    fun `elementDisplacementFor tracks the cumulative drag distance for the dragged row`() {
        // Given an active drag on row 0
        val state = newState()
        state.onDragStart(Offset(0f, 5f)) // initialTop = 0

        // When dragging by a small amount that does not yet trigger a reorder
        state.onDrag(3f)

        // Then the dragged row's displacement equals the dragged distance
        // (initialTop + draggedDistance - rowTops[currentIndex] = 0 + 3 - 0 = 3)
        state.elementDisplacementFor(0) shouldBe 3f
    }

    @Test
    fun `onDragInterrupted notifies the listener with the final index and resets state`() {
        // Given a drag that has reordered row 0 to position 1
        var finalIndex = -2
        val state = newState(onDragFinished = { finalIndex = it })
        state.onDragStart(Offset(0f, 5f))
        state.onDrag(21f) // moves 0 -> 1 (center crosses row 1 midpoint)

        // When the drag is interrupted
        state.onDragInterrupted()

        // Then the listener is notified with the row's current index
        finalIndex shouldBe 1
        // And the dragged-row state is cleared so subsequent dragless calls are no-ops
        state.elementDisplacementFor(1) shouldBe null
    }

    @Test
    fun `onDragInterrupted with no active drag still notifies with the sentinel index`() {
        // Given a fresh state
        var finalIndex = -2
        val state = newState(onDragFinished = { finalIndex = it })

        // When interrupted without ever starting a drag
        state.onDragInterrupted()

        // Then the listener is told there was no dragged row (index = -1)
        finalIndex shouldBe -1
    }
}
