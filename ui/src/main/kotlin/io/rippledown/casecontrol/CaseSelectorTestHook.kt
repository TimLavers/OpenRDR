package io.rippledown.casecontrol

import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only observation surface for the live processed / cornerstone
 * case counts shown by [CaseSelector].
 *
 * Same rationale and pattern as `ChatTestHook` and `CornerstoneTestHook`:
 * the Compose-Desktop accessibility bridge does not reliably expose the
 * `CaseSelector`'s section header text to AWT [javax.accessibility.AccessibleContext],
 * so cucumber scenarios cannot read the visible `"Processed (N)"` and
 * `"Cornerstones (N)"` counts by walking the accessibility tree.
 *
 * `OpenRDRUI` publishes the latest `CasesInfo`-derived counts to this
 * object inside a `SideEffect`, so readers always observe values
 * consistent with the most-recently-rendered frame.
 */
object CaseSelectorTestHook {

    data class Snapshot(
        val processedCount: Int,
        val cornerstoneCount: Int,
        val isShowing: Boolean
    ) {
        companion object {
            val EMPTY = Snapshot(0, 0, false)
        }
    }

    private val snapshotRef = AtomicReference(Snapshot.EMPTY)

    fun update(processedCount: Int, cornerstoneCount: Int, isShowing: Boolean) {
        snapshotRef.set(Snapshot(processedCount, cornerstoneCount, isShowing))
    }

    fun snapshot(): Snapshot = snapshotRef.get()

    fun reset() {
        snapshotRef.set(Snapshot.EMPTY)
    }
}
