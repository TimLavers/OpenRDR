package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.main.EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_FIELD_DESCRIPTION
import io.rippledown.constants.main.EDIT_CONDITION_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import javax.accessibility.AccessibleRole

open class EditConditionOperator(val dialog: ComposeDialog) {

    fun clickOkButton() = dialog.accessibleContext.findAndClick(EDIT_CONDITION_OK_BUTTON_DESCRIPTION)

    fun clickCancelButton() = dialog.accessibleContext.findAndClick(EDIT_CONDITION_CANCEL_BUTTON_DESCRIPTION)

    fun enterValue(value: String) {
        val context = dialog.accessibleContext.find(EDIT_CONDITION_FIELD_DESCRIPTION, AccessibleRole.TEXT)
        context!!.accessibleEditableText.setTextContents(value)
    }
}