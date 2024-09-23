package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
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
        expandDropdownMenu()
        Thread.sleep(1_000)
        clickDropdownItem(CREATE_KB_TEXT)
        Thread.sleep(1_000)
        val dialog = findComposeDialogThatIsShowing()
        val createKbOperator = CreateKbOperator(dialog!!)
        createKbOperator.createKB(name)
    }

    fun createKBFromSample(name: String, sampleTitle: String) {
        expandDropdownMenu()
        Thread.sleep(1_000)
        clickDropdownItem(CREATE_KB_FROM_SAMPLE_TEXT)
        Thread.sleep(1_000)
        val dialog = waitForComposeDialogToShow()
        val createKbOperator = CreateKbFromSampleOperator(dialog)
        createKbOperator.createKbFromSample(name, sampleTitle)
    }

    fun selectKB(name: String) {
        expandDropdownMenu()
        Thread.sleep(1_000)
        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)!!
        Thread.sleep(1_000)
        val menuItem = dropDown.findByName(name, AccessibleRole.LABEL)!!
        menuItem.accessibleAction.doAccessibleAction(0)
        Thread.sleep(1_000)
    }

    fun importKB(filePath: String) {
        Thread.sleep(100)
        expandDropdownMenu()
        Thread.sleep(2000)
        clickDropdownItem(IMPORT_KB_TEXT)
        Thread.sleep(2000)
        SwingUtilities.invokeAndWait {
            val dialog = findComposeDialogThatIsShowing()
            val importKbOperator = ImportKbOperator(dialog!!)
            importKbOperator.importKB(filePath)
        }
    }

    fun exportKB(filePath: String) {
        Thread.sleep(100)
        expandDropdownMenu()
        Thread.sleep(100)
        clickDropdownItem(EXPORT_KB_TEXT)
        Thread.sleep(1000)
        SwingUtilities.invokeAndWait {
            val dialog = findComposeDialogThatIsShowing()
            val exportKbOperator = ExportKbOperator(dialog!!)
            exportKbOperator.importKB(filePath)
        }
    }

    fun availableKBs(): List<String> {
        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)!!
        return dropDown.findLabelChildren()
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