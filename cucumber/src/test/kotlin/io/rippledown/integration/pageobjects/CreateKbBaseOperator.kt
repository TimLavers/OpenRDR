package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.main.CREATE_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.printActions
import javax.accessibility.AccessibleRole

open class CreateKbBaseOperator(val dialog: ComposeDialog) {

   fun clickCreateButton() = dialog.accessibleContext.findAndClick(CREATE_KB_OK_BUTTON_DESCRIPTION)

    fun enterName(name: String) {
        val nameFieldContext = dialog.accessibleContext.find(CREATE_KB_NAME_FIELD_DESCRIPTION, AccessibleRole.TEXT)
        nameFieldContext!!.accessibleEditableText.setTextContents(name)
    }
}