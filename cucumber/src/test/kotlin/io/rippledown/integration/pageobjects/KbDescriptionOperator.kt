package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.main.EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION
import io.rippledown.constants.main.EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION
import io.rippledown.constants.main.EXPORT_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import javax.accessibility.AccessibleRole

class KbDescriptionOperator(private val dialog: ComposeDialog) {

    fun description(): String {
        val nameFieldContext = dialog.accessibleContext.find(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION, AccessibleRole.TEXT)
        val textLength = nameFieldContext!!.accessibleText.charCount
        return nameFieldContext.accessibleEditableText.getTextRange(0, textLength)
    }

    fun setDescription(path: String) {
        enterDescription(path)
        Thread.sleep(1000)
        clickOkButton()
        Thread.sleep(1000)
    }

    private fun clickOkButton() = dialog.accessibleContext.findAndClick(EDIT_KB_DESCRIPTION_OK_BUTTON_DESCRIPTION)

    private fun enterDescription(name: String) {
        val nameFieldContext = dialog.accessibleContext.find(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION, AccessibleRole.TEXT)
        nameFieldContext!!.accessibleEditableText.setTextContents(name)
    }
}