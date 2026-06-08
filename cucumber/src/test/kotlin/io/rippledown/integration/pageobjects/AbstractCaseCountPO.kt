package io.rippledown.integration.pageobjects

import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.getComponentTextUsingOCR
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext

/**
 * Reads the live `Processed (N)` / `Cornerstones (N)` counts from the
 * running [io.rippledown.main.OpenRDRUI] by taking a screenshot of the component
 * and using an OCR library to extract the text.
 */
abstract class AbstractCaseCountPO(val contextProvider: () -> AccessibleContext) {

    fun waitForCountOfNumberOfCasesToBe(count: Int) {
        if (currentCount() == count) return
        await().atMost(ofSeconds(20)).until {
            currentCount() == count
        }
    }

    fun currentCount(): Int {
        val lines = getComponentTextUsingOCR(contextForCaseCount())
        if (lines.isEmpty()) {
            return 0
        }
        if (lines[0].contains(countTag())) {
            return try {
                lines[0].substringAfter("(").substringBefore(")").toInt()
            } catch (_: Exception) {
                return 0
            }
        }
        return 0
    }

    abstract fun countTag(): String
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