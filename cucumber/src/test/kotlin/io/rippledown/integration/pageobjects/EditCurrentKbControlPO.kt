package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.EDIT_CURRENT_KB_CONTROL_DROPDOWN_BUTTON_DESCRIPTION
import io.rippledown.constants.kb.EDIT_CURRENT_KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.EDIT_KB_DESCRIPTION_BUTTON_TEXT
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.findComposeDialogThatIsShowing
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class EditCurrentKbControlPO(private val contextProvider: () -> AccessibleContext) {

fun showDescriptionOperator(): KbDescriptionOperator {
        expandDropdownMenu()
        Thread.sleep(1_000)
        clickDropdownItem(EDIT_KB_DESCRIPTION_BUTTON_TEXT)
        Thread.sleep(1_000)
        val dialog = findComposeDialogThatIsShowing()
        return KbDescriptionOperator(dialog!!)
    }

    private fun expandDropdownMenu() {
        execute { contextProvider().findAndClick(EDIT_CURRENT_KB_CONTROL_DROPDOWN_BUTTON_DESCRIPTION) }
    }

    private fun clickDropdownItem(description: String) {
        execute {
            val dropDown = contextProvider().find(EDIT_CURRENT_KB_CONTROL_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
            dropDown!!.findAndClick(description)
        }
    }
}