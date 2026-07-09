package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findExact
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

/**
 * Reads the live `Processed (N)` / `Cornerstones (N)` counts from the
 * running [io.rippledown.main.OpenRDRUI] by counting the case-name nodes in
 * the relevant section of the accessibility tree. This is the same source the
 * `(N)` label in the section header is derived from, and is far more reliable
 * than OCR-reading the header text.
 */
abstract class AbstractCaseCountPO(val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        if (currentCount() == count) return
        await().atMost(ofSeconds(20)).until {
            currentCount() == count
        }
    }

    fun currentCount(): Int = execute<Int> {
        val section = contextProvider().findExact(sectionDescription()) ?: return@execute 0
        section.findAllByDescriptionPrefix(CASE_NAME_PREFIX).size
    }

    abstract fun sectionDescription(): String
    abstract fun contextDescription(): String

    fun contextForCaseCount(): AccessibleContext? = execute<AccessibleContext?> { contextProvider().find(contextDescription()) }

    fun requireCaseCountToBeHidden() {
        await().atMost(ofSeconds(5)).until { contextForCaseCount() == null }
    }

    fun requireCasesLabelToBeHidden() {
        await().atMost(ofSeconds(5)).until { contextForCaseCount() == null }
    }

    fun requireCasesLabelToBeShown() {
        await().atMost(ofSeconds(5)).until { contextForCaseCount() != null }
    }

    fun requireCaseCountToBeShown() {
        await().atMost(ofSeconds(5)).until { contextForCaseCount() != null }
    }
}