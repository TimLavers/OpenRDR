package io.rippledown.integration.pageobjects

import io.rippledown.casecontrol.CaseSelectorTestHook
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

/**
 * Reads the live `Processed (N)` / `Cornerstones (N)` counts from the
 * running [io.rippledown.main.OpenRDRUI] via [CaseSelectorTestHook].
 *
 * We don't walk the AWT accessibility tree because Compose-Desktop does
 * not reliably expose the section header `Text` nodes there (see
 * [CaseSelectorTestHook] kdoc). The `contextProvider` is retained for
 * subclasses but is no longer used to read the count.
 */
abstract class AbstractCaseCountPO(val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        if (currentCount() == count) return
        await().atMost(ofSeconds(20)).until {
            currentCount() == count
        }
    }

    /** Return the count of this section from the live test hook. */
    protected abstract fun currentCount(): Int

    /** True when this section is currently visible in the UI. */
    protected abstract fun isShowing(): Boolean

    fun requireCaseCountToBeHidden() {
        await().atMost(ofSeconds(5)).until { !isShowing() }
    }

    fun requireCasesLabelToBeHidden() {
        await().atMost(ofSeconds(5)).until { !isShowing() }
    }

    fun requireCasesLabelToBeShown() {
        await().atMost(ofSeconds(5)).until { isShowing() }
    }

    fun requireCaseCountToBeShown() {
        await().atMost(ofSeconds(5)).until { isShowing() }
    }

    protected fun snapshot(): CaseSelectorTestHook.Snapshot = CaseSelectorTestHook.snapshot()
}