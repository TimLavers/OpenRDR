package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.main.EXPORT_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.EXPORT_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import javax.accessibility.AccessibleRole

class ExportKbOperator(private val dialog: ComposeDialog) {

    fun importKB(path: String) {
        enterPath(path)
        Thread.sleep(1000)
        clickExportButton()
        Thread.sleep(1000)
    }

    private fun clickExportButton() = dialog.accessibleContext.findAndClick(EXPORT_KB_OK_BUTTON_DESCRIPTION)

    private fun enterPath(name: String) {
        val nameFieldContext = dialog.accessibleContext.find(EXPORT_KB_NAME_FIELD_DESCRIPTION, AccessibleRole.TEXT)
        nameFieldContext!!.accessibleEditableText.setTextContents(name)
    }
}