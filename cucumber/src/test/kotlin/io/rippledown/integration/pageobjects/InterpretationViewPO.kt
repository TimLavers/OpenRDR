package io.rippledown.integration.pageobjects

import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
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
import javax.accessibility.AccessibleState

// ORD2
class InterpretationViewPO(private val contextProvider: () -> AccessibleContext) {

    fun setVerifiedText(text: String): InterpretationViewPO {
        waitForTextFieldToBeAccessible()
        execute { interpretationTextContext()?.accessibleEditableText?.setTextContents(text) }
        waitForDebounce()
        return this
    }

    private fun interpretationTextContext() = contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)

    private fun waitForTextFieldToBeAccessible() {
        waitUntilAssertedOnEventThread {
            interpretationTextContext() shouldNotBe null
        }
    }

    fun addVerifiedTextAtEndOfCurrentInterpretation(text: String): InterpretationViewPO {
        val newVerifiedText = interpretationText() + " $text"
        setVerifiedText(newVerifiedText)
        waitForDebounce()
        return this
    }

    fun interpretationText(): String = execute<String> {
        contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)!!.accessibleName ?: ""
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
        require(expected >= 1) {
            "To use this method, badge count must be greater than or equal to 1, but was $expected"
        }
        val badgeCountString = diffTabProvider().getAccessibleChild(0).accessibleContext.accessibleName
        try {
            badgeCountString.toInt() shouldBe expected
        } catch (e: NumberFormatException) {
            throw AssertionError("Badge count '$badgeCountString' is not a number")
        }
    }

    fun requireNoBadgeCount() {
        withClue("This is a bit obscure. If there is no badge, the child is the tab label") {
            diffTabProvider().accessibleChildrenCount shouldBe 1
            diffTabProvider().getAccessibleChild(0).accessibleContext.accessibleName shouldBe INTERPRETATION_TAB_CHANGES_LABEL
        }
    }

    fun waitForNoBadgeCount() {
        waitForDebounce()
        waitUntilAssertedOnEventThread { requireNoBadgeCount() }
    }

    fun waitForBadgeCount(expected: Int) {
        waitForDebounce()
        waitUntilAssertedOnEventThread { requireBadgeCount(expected) }
    }

    private fun interpretationTabProvider() = contextProvider().find(INTERPRETATION_TAB_ORIGINAL)!!
    private fun diffTabProvider() = contextProvider().find(INTERPRETATION_TAB_CHANGES)!!
    private fun conclusionsTabProvider() = contextProvider().find(INTERPRETATION_TAB_CONCLUSIONS)!!

    fun selectOriginalTab() = execute { interpretationTabProvider().accessibleAction.doAccessibleAction(0) }
    fun selectConclusionsTab() = execute { conclusionsTabProvider().accessibleAction.doAccessibleAction(0) }
    fun selectDifferencesTab() = execute { diffTabProvider().accessibleAction.doAccessibleAction(0) }

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

    fun requireChangesLabel(expected: String): InterpretationViewPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).text shouldBe expected
        return this
    }

    fun requireNoBadge(): InterpretationViewPO {
//        driver.findElement(By.className(BADGE_INVISIBLE_CLASS)) shouldNotBe null
        return this
    }

    fun buildRule(row: Int) {
        clickBuildIconOnRow(row)
        clickFinishRuleButton()
        waitForDebounce()
    }

    private fun waitForFinishButtonToBeShowing() {
        waitUntilAssertedOnEventThread {
            contextProvider().find(FINISH_RULE_BUTTON)?.accessibleStateSet?.contains(AccessibleState.SHOWING) shouldBe true
        }
    }

    private fun clickFinishRuleButton() {
        waitForFinishButtonToBeShowing()
        execute { contextProvider().find(FINISH_RULE_BUTTON)?.accessibleAction?.doAccessibleAction(0) }
    }

    fun clickBuildIconOnRow(row: Int) {
        waitForNumberOfRowsToBeAtLeast(row + 1)
        waitForBuildIconToBeShowing(row)
        execute { buildIconContext(row)?.accessibleAction?.doAccessibleAction(0) }
    }

    private fun waitForBuildIconToBeShowing(row: Int) {
        waitUntilAssertedOnEventThread {
            buildIconContext(row) shouldNotBe null
        }
    }

    private fun buildIconContext(row: Int) = contextProvider().find("$ICON_PREFIX$row")
}