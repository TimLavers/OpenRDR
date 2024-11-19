package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.interpretation.*
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findComposeDialogThatIsShowing
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.awt.Rectangle
import java.awt.Robot
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.swing.SwingUtilities.invokeLater

// ORD2
class InterpretationPO(private val contextProvider: () -> AccessibleContext) {

    private fun interpretationTextContext() =
        execute<AccessibleContext?> { contextProvider().find(INTERPRETATION_TEXT_FIELD) }

    fun movePointerToComment(comment: String) {
        val interpretation = interpretationText()
        val index = interpretation.indexOf(comment)
        movePointerToCharacterPosition(index)
    }

    fun movePointerToCharacterPosition(characterPosition: Int) {
        val interpretationTextContext = interpretationTextContext()
        val rectangle =
            execute<Rectangle> { interpretationTextContext?.accessibleText?.getCharacterBounds(characterPosition) }
        val loc = interpretationTextContext.accessibleComponent.locationOnScreen
        Robot().mouseMove(loc.x + rectangle.x, loc.y)
    }

    fun interpretationText(): String = execute<String> {
        contextProvider().find(INTERPRETATION_TEXT_FIELD)?.accessibleName ?: ""
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
        await().atMost(ofSeconds(2)).until {
            interpretationText().contains(expected)
        }
    }

    fun clickChangeInterpretationButton() {
        waitUntilAsserted {
            execute<AccessibleContext?> { contextProvider().find(CHANGE_INTERPRETATION_BUTTON) } shouldNotBe null
        }
        execute { contextProvider().find(CHANGE_INTERPRETATION_BUTTON)!!.accessibleAction.doAccessibleAction(0) }
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

    fun waitForConditionsToBeShowing(conditions: List<String>) {
        waitUntilAsserted {
            conditions.forEach { condition ->
                execute<AccessibleContext?> { contextProvider().find(CONDITION_PREFIX + condition) } shouldNotBe null
            }
        }
    }

    fun requireNoConditionsToBeShowing() {
        execute<Set<AccessibleContext>> { contextProvider().findAllByDescriptionPrefix(CONDITION_PREFIX) } shouldHaveSize 0
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
