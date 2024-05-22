package io.rippledown.integration.pageobjects

import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CHANGES
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.waitForDebounce
import io.rippledown.integration.waitUntilAssertedOnEventThread
import io.rippledown.interpretation.CHANGED_PREFIX
import io.rippledown.interpretation.DIFF_ROW_PREFIX
import io.rippledown.interpretation.ICON_PREFIX
import io.rippledown.interpretation.ORIGINAL_PREFIX
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.TEXT

// ORD2
class InterpretationViewPO(private val contextProvider: () -> AccessibleContext) {

    fun setVerifiedText(text: String): InterpretationViewPO {
        contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)?.accessibleEditableText?.setTextContents(text)
        waitForDebounce()
        return this
    }

    fun addVerifiedTextAtEndOfCurrentInterpretation(text: String): InterpretationViewPO {
        setVerifiedText(interpretationText() + text)
        waitForDebounce()
        return this
    }

    fun interpretationText() = execute<String> {
        contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)?.accessibleName ?: ""
    }

    fun waitForInterpretationText(expected: String): InterpretationViewPO {
        await()
            .atMost(ofSeconds(5))
            .until {
                interpretationText() == expected
            }
        return this
    }

    fun waitForInterpretationTextToContain(expected: String) {
        await().atMost(ofSeconds(5)).until {
            interpretationText().contains(expected)
        }
    }

    fun requireBadgeCount(expected: Int) {
        try {
            val badgeCount = diffTabProvider().getAccessibleChild(0).accessibleContext.accessibleName.toInt()
            badgeCount shouldBe expected
        } catch (e: NumberFormatException) {
            throw AssertionError("Badge count is not a number")
        }
    }

    fun waitForBadgeCount(expected: Int) {
        waitForDebounce()
        waitUntilAssertedOnEventThread { requireBadgeCount(expected) }
    }

    private fun diffTabProvider() = contextProvider()
        .find(INTERPRETATION_TAB_CHANGES)!!

    fun selectDifferencesTab() = diffTabProvider().accessibleAction.doAccessibleAction(0)

    fun selectConclusionsTab(): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CONCLUSIONS)).click()
        return this
    }

    fun selectOriginalTab(): InterpretationViewPO {
        return this
    }

    fun requireOriginalTextInRow(row: Int, text: String) = requireTextInCellInRowWithPrefix(ORIGINAL_PREFIX, row, text)

    fun requireChangedTextInRow(row: Int, text: String) = requireTextInCellInRowWithPrefix(CHANGED_PREFIX, row, text)

    fun numberOfRows() = execute<Int> { contextProvider().findAllByDescriptionPrefix(DIFF_ROW_PREFIX).size }

    fun waitForNumberOfRowsToBeAtLeast(rows: Int) = waitUntilAssertedOnEventThread {
        numberOfRows() shouldBeGreaterThanOrEqualTo rows
    }

    fun requireNoRowsInDiffTable() = numberOfRows() shouldBe 0

    fun requireAddedTextRow(row: Int, text: String) {
        requireNoTextInCellInRowWithPrefix(ORIGINAL_PREFIX, row)
        requireTextInCellInRowWithPrefix(CHANGED_PREFIX, row, text)
    }

    fun requireDeletedTextRow(row: Int, text: String) {
        requireTextInCellInRowWithPrefix(ORIGINAL_PREFIX, row, text)
        requireNoTextInCellInRowWithPrefix(CHANGED_PREFIX, row)
    }

    fun requireReplacedTextRow(row: Int, replaced: String, replacement: String) {
        requireTextInCellInRowWithPrefix(ORIGINAL_PREFIX, row, replaced)
        requireTextInCellInRowWithPrefix(CHANGED_PREFIX, row, replacement)
    }

    private fun requireTextInCellInRowWithPrefix(prefix: String, row: Int, text: String) {
        val found = execute<String> { contextProvider().find("$prefix$row")?.accessibleName }
        found shouldBe text
    }

    private fun requireNoTextInCellInRowWithPrefix(prefix: String, row: Int) {
        val found = execute<String?> { contextProvider().find("$prefix$row", TEXT)?.accessibleName }
        found shouldBe null
    }

    fun deleteAllText() = setVerifiedText("")

//    fun WebElement.selectAllText() = sendKeys(Keys.chord(Keys.CONTROL, "a"))

    fun requireChangesLabel(expected: String): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).text shouldBe expected
        return this
    }

    fun requireNoBadge(): InterpretationViewPO {
//        driver.findElement(By.className(BADGE_INVISIBLE_CLASS)) shouldNotBe null
        return this
    }

//    fun WebElement.delete() = sendKeys(Keys.DELETE, Keys.BACK_SPACE)

    fun buildRule(row: Int) {
        clickBuildIconOnRow(row)
        clickFinishRuleButton()
    }

    private fun clickFinishRuleButton() {
        execute { contextProvider().find(FINISH_RULE_BUTTON)?.accessibleAction?.doAccessibleAction(0) }
    }

    fun clickBuildIconOnRow(row: Int) {
        waitForNumberOfRowsToBeAtLeast(row + 1)
        execute { contextProvider().find("$ICON_PREFIX$row")?.accessibleAction?.doAccessibleAction(0) }
    }
}