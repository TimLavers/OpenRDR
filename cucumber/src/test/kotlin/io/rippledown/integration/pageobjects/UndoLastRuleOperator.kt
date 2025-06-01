package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.kb.CONFIRM_UNDO_LAST_RULE_TEXT_DESCRIPTION
import io.rippledown.constants.kb.UNDO_LAST_RULE_BUTTON_DESCRIPTION
import io.rippledown.constants.main.CANCEL_DESCRIPTION
import io.rippledown.constants.main.EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import javax.accessibility.AccessibleRole

class UndoLastRuleOperator(private val dialog: ComposeDialog) {
    fun undoLastRule() {
        clickUndoButton()
        clickConfirmationButton()
    }

    private fun clickUndoButton() = dialog.accessibleContext.findAndClick(UNDO_LAST_RULE_BUTTON_DESCRIPTION)
    private fun clickConfirmationButton() = dialog.accessibleContext.findAndClick(
        CONFIRM_UNDO_LAST_RULE_TEXT_DESCRIPTION
    )

    fun cancel() = dialog.accessibleContext.findAndClick(CANCEL_DESCRIPTION)
}