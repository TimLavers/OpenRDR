package io.rippledown.cornerstone

import io.rippledown.model.rule.CornerstoneStatus
import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only observation surface for the live state of the cornerstone
 * panel.
 *
 * Same rationale and pattern as `ChatTestHook` in the chat package:
 * cucumber integration tests run in the same JVM as the Compose Desktop
 * UI, and need to poll cornerstone state (current case name, index,
 * total, or "is there a cornerstone showing?") to step scenarios. The
 * AWT/Compose accessibility bridge is prohibitively slow on a window
 * that contains a large case table — a single tree walk routinely
 * blocks the EDT for ~6 s, which not only wastes wall-clock time but
 * also starves the dispatcher coroutine that is supposed to refresh
 * `currentCase` after a rule commit, masking the new interpretation
 * from the next assertion in the scenario.
 *
 * `OpenRDRUI` publishes the latest [CornerstoneStatus] to this object
 * inside a `SideEffect`, so readers always observe values consistent
 * with the most-recently-rendered frame. In production this is one
 * atomic reference write per OpenRDRUI recomposition; when no test is
 * reading there is no behavioural consequence.
 */
object CornerstoneTestHook {

    data class Snapshot(
        val cornerstoneCaseName: String?,
        val indexOfCornerstoneToReview: Int,
        val numberOfCornerstones: Int
    ) {
        val isShowing: Boolean get() = cornerstoneCaseName != null

        companion object {
            val EMPTY = Snapshot(
                cornerstoneCaseName = null,
                indexOfCornerstoneToReview = -1,
                numberOfCornerstones = 0
            )
        }
    }

    private val snapshotRef = AtomicReference(Snapshot.EMPTY)

    fun update(status: CornerstoneStatus?) {
        if (status == null || status.cornerstoneToReview == null) {
            snapshotRef.set(Snapshot.EMPTY)
        } else {
            snapshotRef.set(
                Snapshot(
                    cornerstoneCaseName = status.cornerstoneToReview?.case?.name,
                    indexOfCornerstoneToReview = status.indexOfCornerstoneToReview,
                    numberOfCornerstones = status.numberOfCornerstones
                )
            )
        }
    }

    fun snapshot(): Snapshot = snapshotRef.get()

    fun reset() {
        snapshotRef.set(Snapshot.EMPTY)
    }
}
