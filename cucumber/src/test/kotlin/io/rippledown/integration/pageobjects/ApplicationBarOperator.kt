package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.constants.kb.PRESENCE_DETAILS_DESCRIPTION
import io.rippledown.constants.main.MAIN_HEADING
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findExact
import io.rippledown.integration.utils.renderedText
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

class ApplicationBarOperator(private val contextProvider: () -> AccessibleContext) {

    fun presence(): String? =
        contextProvider().findExact(PRESENCE_DETAILS_DESCRIPTION)?.let(::renderedText)

    fun showPresence(): Boolean = presence() != null ||
            contextProvider().find(PRESENCE_DESCRIPTION, AccessibleRole.PUSH_BUTTON)
                ?.accessibleAction?.doAccessibleAction(0) == true

    fun hidePresence() {
        if (presence() != null) {
            contextProvider().find(PRESENCE_DESCRIPTION, AccessibleRole.PUSH_BUTTON)
                ?.accessibleAction?.doAccessibleAction(0)
        }
    }

    fun title(): String {
        val textContext = contextProvider().find(MAIN_HEADING, AccessibleRole.LABEL)!!
        return renderedText(textContext)
    }
}
