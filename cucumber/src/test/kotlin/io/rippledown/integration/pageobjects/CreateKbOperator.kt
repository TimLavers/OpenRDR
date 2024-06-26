package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.main.CREATE_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.printActions
import javax.accessibility.AccessibleRole

class CreateKbOperator(dialog: ComposeDialog) : CreateKbBaseOperator(dialog){

    fun createKB(name: String) {
        enterName(name)
        Thread.sleep(1000)
        clickCreateButton()
        Thread.sleep(1000)
    }
}