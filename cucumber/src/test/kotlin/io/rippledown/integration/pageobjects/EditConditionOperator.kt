package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_FIELD_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute

open class EditConditionOperator(val dialog: ComposeDialog) {

    fun clickOkButton() = dialog.accessibleContext.findAndClick(EDIT_CONDITION_OK_BUTTON_DESCRIPTION)

    fun clickCancelButton() = dialog.accessibleContext.findAndClick(EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION)

    fun enterValue(value: String) {
        waitUntilAsserted {
            val editField = dialog.accessibleContext.find(EDIT_CONDITION_FIELD_DESCRIPTION)
            if (editField != null) {
                execute {
                    editField.accessibleEditableText.setTextContents(value)
                }
                editField.accessibleName shouldBe value
            }
        }
    }
}