package io.rippledown.integration.pageobjects

import io.rippledown.constants.kb.KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.renderedText
import io.rippledown.integration.waitUntilAsserted
import javax.accessibility.AccessibleContext

class KbControlsPO(private val contextProvider: () -> AccessibleContext) {
    fun currentKB(): String {
        // Capture the rendered text inside the poll: the node can be replaced
        // between lookup and a subsequent re-fetch when the KB changes.
        lateinit var text: String
        waitUntilAsserted {
            val node = contextProvider().find(KB_CONTROL_CURRENT_KB_LABEL_DESCRIPTION)
                ?: throw AssertionError("Current KB label not yet rendered")
            text = renderedText(node)
        }
        return text
    }
}
