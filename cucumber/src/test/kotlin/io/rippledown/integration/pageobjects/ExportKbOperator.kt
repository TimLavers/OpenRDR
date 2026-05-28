package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.EXPORT_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.EXPORT_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import javax.accessibility.AccessibleRole

class ExportKbOperator(private val dialog: ComposeDialog) {

    fun importKB(path: String) {
        enterPath(path)
        Thread.sleep(1000)
        clickExportButton()
        Thread.sleep(1000)
    }

    // The OK button can be absent from the accessibility tree for the first
    // frames after the dialog opens (or be replaced by a freshly composed
    // node). Poll find-and-click as a single atomic step so we don't NPE on
    // a stale node reference.
    private fun clickExportButton() {
        waitUntilAsserted {
            val clicked = run {
                val node = dialog.accessibleContext.find(EXPORT_KB_OK_BUTTON_DESCRIPTION)
                    ?: return@run false
                node.accessibleAction?.doAccessibleAction(0) ?: false
                true
            }
            clicked shouldBe true
        }
    }

    private fun enterPath(name: String) {
        val nameFieldContext = dialog.accessibleContext.find(EXPORT_KB_NAME_FIELD_DESCRIPTION, AccessibleRole.TEXT)
        nameFieldContext!!.accessibleEditableText.setTextContents(name)
    }
}