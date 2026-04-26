package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.SWITCH_KB_HEADER_TEXT
import io.rippledown.constants.main.*
import io.rippledown.integration.utils.*
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.swing.SwingUtilities

class KbControlsPO(private val contextProvider: () -> AccessibleContext) {

    fun requireKbControlsToBeHidden() {
        waitUntilAsserted { contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION) shouldBe null }
    }

    fun requireKbControlsToBeShown() {
        waitUntilAsserted { contextProvider().find(KB_CONTROL_DROPDOWN_DESCRIPTION) shouldNotBe null }
    }

    fun currentKB(): String {
        val textContext = contextProvider().find(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION, AccessibleRole.LABEL)!!
        return textContext.accessibleName
    }

    fun createKB(name: String) {
        openDropdownMenu()
        clickDropdownItem(CREATE_KB_TEXT)
        val dialog = waitForComposeDialogToShow()
        CreateKbOperator(dialog).createKB(name)
    }

    fun createKBFromSample(name: String, sampleTitle: String) {
        openDropdownMenu()
        clickDropdownItem(CREATE_KB_FROM_SAMPLE_TEXT)
        val dialog = waitForComposeDialogToShow()
        CreateKbFromSampleOperator(dialog).createKbFromSample(name, sampleTitle)
    }

    fun selectKB(name: String) {
        // The current KB is shown as the dropdown trigger and is excluded from
        // the switcher list, so "selecting" it again is a no-op.
        if (currentKB() == name) return
        openDropdownMenu()
        // Poll until the named KB appears as a child of the dropdown — the
        // accessibility tree can lag the visual render by a few frames.
        lateinit var menuItem: AccessibleContext
        waitUntilAsserted {
            val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            menuItem = dropDown?.findByName(name, AccessibleRole.LABEL)
                ?: throw AssertionError("KB '$name' not yet available in dropdown")
        }
        menuItem.accessibleAction.doAccessibleAction(0)
    }

    fun importKB(filePath: String) {
        openDropdownMenu()
        clickDropdownItem(IMPORT_KB_TEXT)
        // Wait for the dialog *off* the EDT — `waitForComposeDialogToShow`
        // sleeps in a poll loop, and blocking the EDT would prevent Compose
        // from ever creating the dialog window.
        val dialog = waitForComposeDialogToShow()
        SwingUtilities.invokeAndWait {
            ImportKbOperator(dialog).importKB(filePath)
        }
    }

    fun exportKB(filePath: String) {
        openDropdownMenu()
        clickDropdownItem(EXPORT_KB_TEXT)
        val dialog = waitForComposeDialogToShow()
        SwingUtilities.invokeAndWait {
            ExportKbOperator(dialog).importKB(filePath)
        }
    }

    private fun openDropdownMenu() {
        expandDropdownMenu()
        // Wait for the menu accessibility node to be present before any caller
        // tries to interact with its children.
        waitUntilAsserted {
            contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX) shouldNotBe null
        }
    }

    fun availableKBs(): List<String> {
        // The dropdown may not have rendered yet when this is called; return
        // an empty list so callers using `waitUntilAsserted` see an
        // AssertionError (not an NPE) and keep polling.
        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            ?: return emptyList()
        // The dropdown has a non-selectable section header above the list of
        // KBs ("Switch knowledge base"); strip it out so callers only see the
        // actual KB names.
        return dropDown.findLabelChildren().filter { it != SWITCH_KB_HEADER_TEXT }
    }

    fun expandDropdownMenu() {
        execute { contextProvider().findAndClick(KB_CONTROL_DROPDOWN_DESCRIPTION) }
    }

    private fun clickDropdownItem(description: String) {
        execute {
            val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            dropDown!!.findAndClick(description)
        }
    }
}