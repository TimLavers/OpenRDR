package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_CONTROL_DROPDOWN_DESCRIPTION
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.CREATE_KB_TEXT
import io.rippledown.constants.main.KBS_DROPDOWN_DESCRIPTION
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.*
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class KbControlOperator(private val contextProvider: () -> AccessibleContext) {

    fun currentKB(): String {
        val textContext = contextProvider().find(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION, AccessibleRole.LABEL)!!
        return textContext.accessibleName
    }

    fun createKB(name: String) {
        expandDropdownMenu()
        println("clicked, init")

        Thread.sleep(3_000)
        clickCreateKbButton()


        Thread.sleep(1_000)
        val dialog = findComposeDialogThatIsShowing()
        val createKbOperator = CreateKbOperator(dialog!!)
        createKbOperator.createKB(name)
    }

    fun availableKBs(): List<String> {
        contextProvider().dumpToText()

        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)!!


        return dropDown.findLabelChildren()
    }


    public fun expandDropdownMenu()  = contextProvider().findAndClick(KB_CONTROL_DROPDOWN_DESCRIPTION)
    public fun clickCreateKbButton() {
        println("========================================================")
        contextProvider().dumpToText()
        val dropDown = contextProvider().find(KBS_DROPDOWN_DESCRIPTION, AccessibleRole.COMBO_BOX)
        println("dropDown: $dropDown")
        dropDown!!.findAndClick(CREATE_KB_TEXT)

    }
}