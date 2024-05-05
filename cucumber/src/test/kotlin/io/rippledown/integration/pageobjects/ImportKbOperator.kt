package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.main.IMPORT_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.IMPORT_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import javax.accessibility.AccessibleRole

class ImportKbOperator(private val dialog: ComposeDialog) {

    fun importKB(path: String) {
        enterPath(path)
        Thread.sleep(1000)
        clickImportButton()
        Thread.sleep(1000)
    }

    private fun clickImportButton() = dialog.accessibleContext.findAndClick(IMPORT_KB_OK_BUTTON_DESCRIPTION)

    private fun enterPath(name: String) {
        val nameFieldContext = dialog.accessibleContext.find(IMPORT_KB_NAME_FIELD_DESCRIPTION, AccessibleRole.TEXT)
        nameFieldContext!!.accessibleEditableText.setTextContents(name)
    }
}