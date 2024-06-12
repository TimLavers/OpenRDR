package io.rippledown.integration.pageobjects

import androidx.compose.ui.awt.ComposeDialog
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.main.CREATE_KB_NAME_FIELD_DESCRIPTION
import io.rippledown.constants.main.CREATE_KB_OK_BUTTON_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.utils.findAndClickRadioButton
import io.rippledown.integration.utils.printActions
import javax.accessibility.AccessibleRole

class CreateKbFromSampleOperator(dialog: ComposeDialog) : CreateKbBaseOperator(dialog){

    fun createKbFromSample(name: String, sampleTitle: String) {
        dialog.accessibleContext.findAndClickRadioButton("select $sampleTitle")
        Thread.sleep(100)
        enterName(name)
        Thread.sleep(100)
        clickCreateButton()
        Thread.sleep(1000)
    }
}