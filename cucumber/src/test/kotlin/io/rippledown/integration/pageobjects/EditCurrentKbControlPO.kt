package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.kb.*
import io.rippledown.constants.main.*
import io.rippledown.integration.utils.*
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class EditCurrentKbControlPO(private val contextProvider: () -> AccessibleContext) {

    fun requireHidden() {
        waitUntilAsserted { contextProvider().find(EDIT_CURRENT_KB_CONTROL_DESCRIPTION) shouldBe null }
    }

    fun requireShowing() {
        waitUntilAsserted { contextProvider().find(EDIT_CURRENT_KB_CONTROL_DESCRIPTION) shouldNotBe null }
    }

    fun setKbDescription(name: String) {
        expandDropdownMenu()
        Thread.sleep(1_000)
        clickDropdownItem(EDIT_KB_DESCRIPTION_TEXT_DESCRIPTION)
        Thread.sleep(1_000)
        val dialog = findComposeDialogThatIsShowing()
        val createKbOperator = CreateKbOperator(dialog!!)
        createKbOperator.createKB(name)
    }

    fun kbDescription(): String {
        expandDropdownMenu()
        Thread.sleep(1_000)
        clickDropdownItem(EDIT_KB_DESCRIPTION_BUTTON_TEXT)
        Thread.sleep(1_000)
        val dialog = findComposeDialogThatIsShowing()
        val createKbOperator = KbDescriptionOperator(dialog!!)
        return createKbOperator.description()
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