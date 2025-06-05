package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.kb.CLOSE_SHOW_LAST_RULE_DESCRIPTION
import io.rippledown.constants.kb.CONFIRM_UNDO_LAST_RULE_YES_OPTION_DESCRIPTION
import io.rippledown.constants.kb.LAST_RULE_DESCRIPTION_DESCRIPTION
import io.rippledown.constants.kb.UNDO_LAST_RULE_BUTTON_DESCRIPTION
import io.rippledown.constants.main.CANCEL_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleRole

class UndoLastRuleOperator(private val dialog: ComposeDialog) {
    fun undoLastRule() {
        clickUndoButton()
        clickConfirmationButton()
    }

    fun ruleDescription(): String {
        val fieldContext = dialog.accessibleContext.find(LAST_RULE_DESCRIPTION_DESCRIPTION, AccessibleRole.TEXT)
        val textLength = fieldContext!!.accessibleText.charCount
        return fieldContext.accessibleEditableText.getTextRange(0, textLength)
    }

    fun checkThatNoRuleCanBeUndone() {

    }

    private fun clickUndoButton() = dialog.accessibleContext.findAndClick(UNDO_LAST_RULE_BUTTON_DESCRIPTION)

    private fun clickConfirmationButton() {
        waitUntilAsserted() {
            execute { dialog.accessibleContext.find(CONFIRM_UNDO_LAST_RULE_YES_OPTION_DESCRIPTION)!!.accessibleAction.doAccessibleAction(0) }

        }
        dialog.accessibleContext.findAndClick(
            CONFIRM_UNDO_LAST_RULE_YES_OPTION_DESCRIPTION
        )
    }

    fun cancel() = dialog.accessibleContext.findAndClick(CLOSE_SHOW_LAST_RULE_DESCRIPTION)
}