package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.IMPORT_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.IMPORT_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.enterTextIntoTextField
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted

class ImportKbOperator(private val dialog: ComposeDialog) {

    fun importKB(path: String) {
        enterPath(path)
        clickImportButton()
    }

    // The OK button can be absent from the accessibility tree for the first
    // frames after the dialog opens (or be replaced by a freshly composed
    // node). Poll find-and-click as a single atomic step so we don't NPE on
    // a stale node reference.
    private fun clickImportButton() {
        waitUntilAsserted {
            val clicked = run {
                val node = dialog.accessibleContext.find(IMPORT_KB_OK_BUTTON_DESCRIPTION)
                    ?: return@run false
                node.accessibleAction?.doAccessibleAction(0) ?: false
                true
            }
            clicked shouldBe true
        }
    }

    private fun enterPath(path: String) {
        dialog.accessibleContext.enterTextIntoTextField(IMPORT_KB_NAME_FIELD_DESCRIPTION, path)
    }
}