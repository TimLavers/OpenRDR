package io.rippledown.integration.pageobjects

import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {
    private fun interpretationText(): String? = execute<String?> {
        // From Compose 1.11 the Java accessibility bridge uses the
        // contentDescription as the accessible name on Text nodes,
        // overriding the rendered text. Read the rendered text via
        // AccessibleText (which exposes the actual characters) instead.
        val ctx = contextProvider().find(INTERPRETATION_TEXT_FIELD) ?: return@execute null
        val text = ctx.accessibleText ?: return@execute ctx.accessibleName
        buildString {
            for (i in 0 until text.charCount) {
                val ch = text.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, i)
                if (ch != null) append(ch)
            }
        }
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