package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.EXPORT_KB_TEXT
import io.rippledown.constants.main.IMPORT_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.renderedText
import io.rippledown.integration.utils.waitForComposeDialogToShow
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class KbControlsPO(private val contextProvider: () -> AccessibleContext) {

    fun requireKbControlsToBeHidden() {
        waitUntilAsserted { contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION) shouldBe null }
    }

    fun requireKbControlsToBeShown() {
        waitUntilAsserted { contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION) shouldNotBe null }
    }

    fun currentKB(): String {
        // Don't constrain by role: Compose 1.11 may report this merged Text
        // node with a non-LABEL role (it sits inside a TextButton). Match
        // by description only and poll until the AppBar has rendered.
        // Capture the rendered text inside the poll: under Compose 1.11 the
        // node can be replaced between the polling lookup and a subsequent
        // re-fetch (e.g. when the AppBar re-composes after a KB switch).
        lateinit var text: String
        waitUntilAsserted {
            val node = contextProvider().find(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION)
                ?: throw AssertionError("Current KB label not yet rendered")
            text = renderedText(node)
        }
        return text
    }

    fun importKB(filePath: String) {
        openDropdownMenu()
        clickDropdownItem(IMPORT_KB_TEXT)
        // Drive the dialog *off* the EDT: the EDT is inside the modal
        // dialog's event pump, so blocking it stops Compose from applying the
        // typed text or recomposing. Each accessibility access hops to the EDT
        // individually.
        val dialog = waitForComposeDialogToShow()
        ImportKbOperator(dialog).importKB(filePath)
    }

    fun exportKB(filePath: String) {
        openDropdownMenu()
        clickDropdownItem(EXPORT_KB_TEXT)
        val dialog = waitForComposeDialogToShow()
        ExportKbOperator(dialog).importKB(filePath)
    }

    private fun openDropdownMenu() {
        expandDropdownMenu()
        // Wait for the menu accessibility node to be present before any caller
        // tries to interact with its children.
        waitUntilAsserted {
            contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX) shouldNotBe null
        }
    }

    fun expandDropdownMenu() {
        // Find-and-click in a single EDT pass and retry: the dropdown
        // accessibility node can appear in one frame and be replaced by a
        // freshly composed equivalent in the next (e.g. when the AppBar
        // re-renders shortly after launch), so searching and clicking
        // across two `execute { ... }` calls races against the swap.
        waitUntilAsserted {
            val clicked = execute<Boolean> {
                val node = contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION)
                    ?: return@execute false
                node.accessibleAction?.doAccessibleAction(0) ?: false
                true
            }
            clicked shouldBe true
        }
    }

    private fun clickDropdownItem(description: String) {
        execute {
            val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            dropDown!!.findAndClick(description)
        }
    }
}
