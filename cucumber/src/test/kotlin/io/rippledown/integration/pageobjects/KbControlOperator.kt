package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.constants.kb.KB_SELECTOR_ID
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.find
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class KbControlOperator(private val accessibleContext: AccessibleContext) {

    fun currentKB(): String {
        val textContext = accessibleContext.find(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION, AccessibleRole.LABEL)!!
        return textContext.accessibleName
    }
}