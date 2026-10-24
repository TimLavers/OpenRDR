package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.renderedText
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class ApplicationBarOperator(private val contextProvider: () -> AccessibleContext) {

    fun presence(): String? = contextProvider().find(PRESENCE_DESCRIPTION, AccessibleRole.LABEL)?.let(::renderedText)

    fun title(): String {
        val textContext = contextProvider().find(MAIN_HEADING, AccessibleRole.LABEL)!!
        return renderedText(textContext)
    }
}
