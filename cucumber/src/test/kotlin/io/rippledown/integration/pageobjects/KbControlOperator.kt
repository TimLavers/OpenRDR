package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.IMPORT_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.integration.utils.*
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.swing.SwingUtilities

class KbControlOperator(private val contextProvider: () -> AccessibleContext) {

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
        Thread.sleep(100)
        clickDropdownItem(IMPORT_KB_TEXT)
        Thread.sleep(100)
        SwingUtilities.invokeAndWait {
            val dialog = findComposeDialogThatIsShowing()
            val importKbOperator = ImportKbOperator(dialog!!)
            importKbOperator.importKB(filePath)
        }
    }

    fun availableKBs(): List<String> {
        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)!!
        return dropDown.findLabelChildren()
    }

    public fun expandDropdownMenu() {
        SwingUtilities.invokeAndWait{
            contextProvider().findAndClick(KB_CONTROL_DROPDOWN_DESCRIPTION)
        }
    }

    private fun clickDropdownItem(description: String) {
        SwingUtilities.invokeAndWait {
            val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            dropDown!!.findAndClick(description)
        }
    }
}