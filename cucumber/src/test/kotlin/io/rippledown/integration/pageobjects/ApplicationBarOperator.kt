package io.rippledown.integration.pageobjects

import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.renderedText
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class ApplicationBarOperator(private val contextProvider: () -> AccessibleContext) {

    fun title(): String {
        val textContext = contextProvider().find(MAIN_HEADING, AccessibleRole.LABEL)!!
        return renderedText(textContext)
    }
}