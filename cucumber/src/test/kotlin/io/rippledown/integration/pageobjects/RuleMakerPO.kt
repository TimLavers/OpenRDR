package io.rippledown.integration.pageobjects

import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {
    private fun interpretationText(): String? = execute<String?> {
        contextProvider().find(INTERPRETATION_TEXT_FIELD)?.accessibleName
    }

    fun requireMessageForAddingComment(newComment: String) {
        waitUntilAsserted {
            interpretationText() shouldContain newComment
        }
    }

    fun requireMessageForRemovingComment(originalComment: String) {
        waitUntilAsserted {
            interpretationText() shouldContain originalComment
        }
    }

    fun requireMessageForReplacingComment(replacedComment: String, replacementComment: String) {
        waitUntilAsserted {
            val text = interpretationText()
            text shouldContain replacedComment
            text shouldContain replacementComment
        }
    }
}