package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_CONTROL_DESCRIPTION
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.find
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class ApplicationBarOperator(private val accessibleContext: AccessibleContext) {

    fun title(): String {
        val textContext = accessibleContext.find(MAIN_HEADING, AccessibleRole.LABEL)!!
        return textContext.accessibleName
    }

    fun kbControlOperator(): KbControlOperator {
        val rowContext = accessibleContext.find(KB_CONTROL_DESCRIPTION, AccessibleRole.UNKNOWN)
        return KbControlOperator(rowContext!!)
    }

}