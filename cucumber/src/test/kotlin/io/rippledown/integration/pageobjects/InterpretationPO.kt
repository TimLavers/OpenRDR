package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.integration.pause
import io.rippledown.integration.utils.*
import io.rippledown.integration.waitForDebounce
import io.rippledown.integration.waitUntilAsserted
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
import javax.swing.SwingUtilities.invokeLater

// ORD2
class InterpretationPO(private val contextProvider: () -> AccessibleContext) {

    fun setVerifiedText(text: String): InterpretationPO {
        selectOriginalTab()
        waitForTextFieldToBeAccessible()
        val interpretationTextContext = interpretationTextContext()
        execute { interpretationTextContext?.accessibleEditableText?.setTextContents(text) }
        waitForDebounce()
        return this
    }

    private fun interpretationTextContext() =
        execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT) }

    private fun waitForTextFieldToBeAccessible() {
        waitUntilAsserted { interpretationTextContext() shouldNotBe null }
    }

    fun addVerifiedTextAtEndOfCurrentInterpretation(text: String): InterpretationPO {
        val newVerifiedText = interpretationText() + " $text"
        setVerifiedText(newVerifiedText)
        waitForDebounce()
        return this
    }

    fun interpretationText(): String = execute<String> {
        contextProvider().find(INTERPRETATION_TEXT_FIELD, TEXT)!!.accessibleName ?: ""
    }

    fun waitForInterpretationText(expected: String): InterpretationPO {
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
        waitUntilAsserted { requireNoBadgeCount() }
    }

    fun waitForBadgeCount(expected: Int) {
        waitForDebounce()
        waitUntilAsserted { requireBadgeCount(expected) }
    }

    private fun interpretationTabProvider() =
        execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_TAB_ORIGINAL) }

    private fun diffTabProvider() = execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_TAB_CHANGES) }
    private fun conclusionsTabProvider() =
        execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_TAB_CONCLUSIONS) }

    fun selectOriginalTab() {
        waitForContextToBeNotNull(contextProvider, INTERPRETATION_TAB_ORIGINAL)
        val context = interpretationTabProvider()
        execute { context?.accessibleAction?.doAccessibleAction(0) }
    }

    fun selectConclusionsTab() {
        waitForContextToBeNotNull(contextProvider, INTERPRETATION_TAB_CONCLUSIONS)
        val context = conclusionsTabProvider()
        execute { context?.accessibleAction?.doAccessibleAction(0) }
    }

    fun selectDifferencesTab() {
        waitForContextToBeNotNull(contextProvider, INTERPRETATION_TAB_CHANGES)
        val context = diffTabProvider()
        execute { context?.accessibleAction?.doAccessibleAction(0) }
    }

    fun requireOriginalTextInRow(row: Int, text: String) = requireTextInCellInRowWithPrefix(ORIGINAL_PREFIX, row, text)

    fun requireChangedTextInRow(row: Int, text: String) = requireTextInCellInRowWithPrefix(CHANGED_PREFIX, row, text)

    fun numberOfRows() = execute<Int> { contextProvider().findAllByDescriptionPrefix(DIFF_ROW_PREFIX).size }

    fun waitForNumberOfRowsToBeAtLeast(rows: Int) = waitUntilAsserted {
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

    fun requireChangesLabel(expected: String): InterpretationPO {
//        driver.findElement(By.id(INTERPRETATION_TAB_CHANGES)).text shouldBe expected
        return this
    }

    fun requireNoBadge(): InterpretationPO {
//        driver.findElement(By.className(BADGE_INVISIBLE_CLASS)) shouldNotBe null
        return this
    }

    fun buildRule(row: Int) {
        clickBuildIconOnRow(row)
        clickFinishRuleButton()
        waitForDebounce()
    }

    private fun waitForFinishButtonToBeShowing() {
        waitUntilAsserted {
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
        val buildIconContext = buildIconContext(row)
        execute { buildIconContext?.accessibleAction?.doAccessibleAction(0) }
    }

    fun clickChangeInterpretationButton() =
        execute { contextProvider().find(CHANGE_INTERPRETATION_BUTTON)!!.accessibleAction.doAccessibleAction(0) }


    fun clickAddCommentMenu() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(ADD_COMMENT_MENU) } shouldNotBe null
        }
        invokeLater { contextProvider().find(ADD_COMMENT_MENU)!!.accessibleAction.doAccessibleAction(0) }
    }

    fun clickRemoveCommentMenu() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(REMOVE_COMMENT_MENU) } shouldNotBe null
        }
        invokeLater { contextProvider().find(REMOVE_COMMENT_MENU)!!.accessibleAction.doAccessibleAction(0) }
    }
    fun clickReplaceCommentMenu() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(REPLACE_COMMENT_MENU) } shouldNotBe null
        }
        invokeLater { contextProvider().find(REPLACE_COMMENT_MENU)!!.accessibleAction.doAccessibleAction(0) }
    }

    fun setAddCommentTextAndClickOK(comment: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        execute { dialog.accessibleContext.find(NEW_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(comment) }
        execute { dialog.accessibleContext.find(OK_BUTTON_FOR_ADD_COMMENT)!!.accessibleAction.doAccessibleAction(0) }
    }

    private fun waitForBuildIconToBeShowing(row: Int) {
        waitUntilAsserted {
            buildIconContext(row) shouldNotBe null
        }
    }

    fun selectCommentToRemoveAndClickOK(comment: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            execute { findAndClick(DROP_DOWN_TEXT_FIELD) }
            pause(1_000)
            execute { findAndClick("$REMOVE_COMMENT_SELECTOR_PREFIX$comment") }
            execute {
                find(OK_BUTTON_FOR_REMOVE_COMMENT)!!.accessibleAction.doAccessibleAction(0)
            }
        }
    }
    fun selectCommentToReplaceAndEnterItsReplacementAndClickOK(comment: String, replacement: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            execute { findAndClick(DROP_DOWN_TEXT_FIELD) }
            pause(1_000)
            execute { findAndClick("$REPLACE_COMMENT_SELECTOR_PREFIX$comment") }
            pause(1_000)
            execute { find(REPLACEMENT_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(replacement) }
            execute {
                find(OK_BUTTON_FOR_REPLACE_COMMENT)!!.accessibleAction.doAccessibleAction(0)
            }
        }
    }

    private fun buildIconContext(row: Int) = execute<AccessibleContext?> { contextProvider().find("$ICON_PREFIX$row") }
}