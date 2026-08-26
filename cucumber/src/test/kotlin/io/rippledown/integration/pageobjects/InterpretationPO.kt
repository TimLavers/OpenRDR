package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import io.rippledown.integration.utils.*
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.awt.Dimension
import java.awt.Point
import java.awt.Robot
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities.invokeLater

// ORD2
class InterpretationPO(private val contextProvider: () -> AccessibleContext) {

    /**
     * Move the pointer over the row showing the given comment, which is what
     * shows that comment's conditions.
     */
    fun movePointerToComment(comment: String) {
        waitForInterpretationTextToContain(comment)
        val cell = commentCellShowing(comment)
            ?: throw AssertionError("No comment row showing \"$comment\". Showing: ${commentsShown()}")
        movePointerOverCentreOf(cell)
    }

    /**
     * The comments shown by the Comments table, in the order of its rows,
     * including any comment that the rule being built is about to add or to put
     * in place of another.
     */
    fun commentsShown(): List<String> =
        execute<List<String>> {
            commentCells().map { renderedText(it) }
        }

    /**
     * The comments of the table read as one string, as they were shown before
     * the comments became a list, so that an expectation of the whole
     * interpretation can still be written as one string.
     */
    fun interpretationText(): String = commentsShown().joinToString(" ")

    /**
     * Requires the table to show these comments, in this order, and no others.
     * Read as a list rather than as one string so that a comment shown twice,
     * or shown alongside another rendering of itself, is a failure.
     */
    fun requireCommentsShownToBe(expected: List<String>) {
        waitUntilAsserted {
            commentsShown() shouldBe expected
        }
    }

    /**
     * The names shown in the name column, in the order of the rows, being the
     * names of the comment attributes that gave the comments.
     */
    fun commentNamesShown(): List<String> =
        execute<List<String>> {
            contextProvider().findAllByDescriptionPrefixesInOrder(
                COMMENT_NAME_PREFIX,
                COMMENT_REPLACEMENT_NAME_PREFIX
            ).map { renderedText(it) }
        }

    /**
     * The name shown beside the given comment, or null if the comment is not
     * shown. The name and text cells of a row are read in the same order, so
     * they can be paired off.
     */
    fun nameShownForComment(comment: String): String? {
        val names = commentNamesShown()
        val index = commentsShown().indexOfFirst { it == comment }
        return if (index in names.indices) names[index] else null
    }

    fun waitForCommentToBeNamed(comment: String, name: String) {
        waitUntilAsserted {
            withClue("the name shown for the comment \"$comment\", of ${commentsShown()} named ${commentNamesShown()}") {
                nameShownForComment(comment) shouldBe name
            }
        }
    }

    private fun commentCells(): List<AccessibleContext> =
        contextProvider().findAllByDescriptionPrefixesInOrder(
            COMMENT_TEXT_PREFIX,
            COMMENT_PENDING_ADD_PREFIX,
            COMMENT_PENDING_REMOVE_PREFIX,
            COMMENT_PENDING_REPLACE_PREFIX,
            COMMENT_REPLACEMENT_TEXT_PREFIX
        )

    private fun commentCellShowing(comment: String): AccessibleContext? =
        execute<AccessibleContext?> {
            commentCells().firstOrNull { renderedText(it) == comment }
                ?: commentCells().firstOrNull { renderedText(it).contains(comment) }
        }

    private fun movePointerOverCentreOf(context: AccessibleContext) {
        val component = context.accessibleComponent ?: return
        val location = execute<Point> { component.locationOnScreen } ?: return
        val size = execute<Dimension> { component.size } ?: return
        Robot().mouseMove(location.x + size.width / 2, location.y + size.height / 2)
    }

    fun waitForInterpretationText(expected: String): InterpretationPO {
        // Track the most recent observed text so we can include it in
        // the timeout failure message — helps distinguish "the UI never
        // received the update" from "the UI received the update but the
        // string doesn't exactly match" (trailing whitespace, newline,
        // etc.). Without this diagnostic the scenario fails with just an
        // Awaitility timeout and no indication of the actual content.
        val lastObserved = java.util.concurrent.atomic.AtomicReference("<never read>")
        try {
            await()
                .atMost(ofSeconds(30))
                .until {
                    val actual = interpretationText()
                    lastObserved.set(actual)
                    actual == expected
                }
        } catch (e: org.awaitility.core.ConditionTimeoutException) {
            throw AssertionError(
                "waitForInterpretationText timed out.\n" +
                        "  expected : ${expected.escape()}\n" +
                        "  lastSeen : ${lastObserved.get().escape()}",
                e
            )
        }
        return this
    }

    private fun String.escape(): String =
        "\"" + this.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""

    fun waitForInterpretationTextToContain(expected: String) {
        await().atMost(ofSeconds(30)).until {
            interpretationText().contains(expected)
        }
    }
    fun requireChangeInterpretationIconToBeHidden() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_ICON) } shouldBe null
        }
    }

    fun requireChangeInterpretationIconToBeShowing() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_ICON) } shouldNotBe null
        }
    }

    fun requireChangeInterpretationDropDownMenuToBeShowing() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_DROPDOWN) } shouldNotBe null
        }
    }

    fun requireChangeInterpretationDropDownMenuToBeHidden() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_DROPDOWN) } shouldBe null
        }
    }

    fun clickChangeInterpretationButton() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_ICON) } shouldNotBe null
        }
        execute { contextProvider().find(CHANGE_INTERPRETATION_ICON)!!.accessibleAction.doAccessibleAction(0) }
    }

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
        execute { dialog.accessibleContext.find(ADD_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(comment) }
        execute { dialog.accessibleContext.find(OK_BUTTON_FOR_ADD_COMMENT)!!.accessibleAction.doAccessibleAction(0) }
    }
    fun setAddCommentTextAndClickCancel(comment: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        execute { dialog.accessibleContext.find(ADD_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(comment) }
        execute { dialog.accessibleContext.find(CANCEL_BUTTON_FOR_ADD_COMMENT)!!.accessibleAction.doAccessibleAction(0) }
    }

    fun waitForConditionsToBeShowing(conditions: List<String>) {
        waitUntilAsserted {
            conditions.forEach { condition ->
                execute<AccessibleContext> { contextProvider().find("$CONDITION_PREFIX$condition") } shouldNotBe null
            }
        }
    }

    fun waitForConditionsForComment(comment: String, conditions: List<String>) {
        waitUntilAsserted {
            // Re-trigger the mouse move on each retry to ensure the tooltip appears
            commentCellShowing(comment)?.let { movePointerOverCentreOf(it) }
            conditions.forEach { condition ->
                execute<AccessibleContext> { contextProvider().find("$CONDITION_PREFIX$condition") } shouldNotBe null
            }
        }
    }

    fun requireNoConditionsToBeShowing() {
        execute<Set<AccessibleContext>> { contextProvider().findAllByDescriptionPrefix(CONDITION_PREFIX) } shouldHaveSize 0
    }

    // ── Derived attributes panel ─────────────────────────────────────────────

    fun waitForDerivedValueToBeShown(attributeName: String, expectedValue: String) {
        waitUntilAsserted {
            val rowCtx = execute<AccessibleContext?> {
                contextProvider().find("$DERIVED_VALUE_ROW_PREFIX$attributeName")
            }
            rowCtx shouldNotBe null
            val valueCtx = execute<AccessibleContext?> {
                rowCtx!!.findLabelByRenderedText(expectedValue)
            }
            valueCtx shouldNotBe null
        }
    }

    private fun movePointerOverDerivedValueName(attributeName: String) {
        val nameCtx = execute<AccessibleContext?> {
            contextProvider().find("$DERIVED_VALUE_NAME_PREFIX$attributeName")
        } ?: error("Derived value name not found: $attributeName")
        val component = nameCtx.accessibleComponent
            ?: error("Derived value name has no accessible component: $attributeName")
        val location = execute<Point> { component.locationOnScreen }
        val size = execute<Dimension> { component.size }
        if (location != null && size != null) {
            Robot().mouseMove(location.x + size.width / 2, location.y + size.height / 2)
        }
    }

    fun waitForDerivedValueFormula(attributeName: String, formula: String) {
        val normalizedExpected = formula.filter { !it.isWhitespace() }.replace("**", "").replace("^", "")
        waitUntilAsserted {
            movePointerOverDerivedValueName(attributeName)
            val ctx = execute<AccessibleContext?> {
                contextProvider().find(DERIVED_VALUE_FORMULA_PREFIX)
            }
            ctx shouldNotBe null
            val actual = execute<String> { renderedText(ctx!!) }
            actual.filter { !it.isWhitespace() } shouldBe normalizedExpected
        }
    }

    fun waitForDerivedValueConditions(attributeName: String, conditions: List<String>) {
        waitUntilAsserted {
            movePointerOverDerivedValueName(attributeName)
            conditions.forEach { condition ->
                val ctx = execute<AccessibleContext?> {
                    contextProvider().find("$DERIVED_VALUE_CONDITIONS_PREFIX$condition")
                }
                ctx shouldNotBe null
            }
        }
    }

    fun waitForDerivedValuesEmptyState() {
        waitUntilAsserted {
            val toggleCtx = execute<AccessibleContext?> {
                contextProvider().find(DERIVED_VALUES_TOGGLE)
            }
            toggleCtx shouldNotBe null
            val emptyCtx = execute<AccessibleContext?> {
                contextProvider().find(DERIVED_ATTRIBUTES_NONE)
            }
            emptyCtx shouldNotBe null
        }
    }

    fun selectExistingCommentToAddClickOK(comment: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            execute {
                find(ADD_COMMENT_PREFIX + comment)!!.accessibleAction.doAccessibleAction(0)
            }
            execute { find(OK_BUTTON_FOR_ADD_COMMENT)!!.accessibleAction.doAccessibleAction(0) }

        }
    }

    fun selectCommentToRemoveAndClickOK(comment: String) {
        waitUntilAsserted {
            execute<ComposeDialog> { findComposeDialogThatIsShowing() } shouldNotBe null
        }
        val dialog = execute<ComposeDialog> { findComposeDialogThatIsShowing() }
        with(dialog.accessibleContext) {
            execute {
                dialog.accessibleContext.find(REMOVE_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(
                    comment
                )
            }
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
            //Enter the comment to be replaced
            execute { find(REPLACED_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(comment) }

            //Enter replacement comment
            execute { find(REPLACEMENT_COMMENT_TEXT_FIELD)!!.accessibleEditableText.setTextContents(replacement) }

            //Click OK
            waitUntilAsserted {
                find(OK_BUTTON_FOR_REPLACE_COMMENT) shouldNotBe null
            }
            execute {
                find(OK_BUTTON_FOR_REPLACE_COMMENT)!!.accessibleAction.doAccessibleAction(0)
            }
        }
    }
}
