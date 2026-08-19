package io.rippledown.integration.pageobjects

import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.interpretation.*
import io.rippledown.integration.utils.findAllByDescriptionPrefixesInOrder
import io.rippledown.integration.utils.renderedText
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

class RuleMakerPO(private val contextProvider: () -> AccessibleContext) {

    /**
     * The comments shown by the Comments table, including the one the rule being
     * built is about to add or to put in place of another, read as one string.
     */
    private fun interpretationText(): String = execute<String> {
        contextProvider().findAllByDescriptionPrefixesInOrder(
            COMMENT_TEXT_PREFIX,
            COMMENT_PENDING_ADD_PREFIX,
            COMMENT_PENDING_REMOVE_PREFIX,
            COMMENT_PENDING_REPLACE_PREFIX,
            COMMENT_REPLACEMENT_TEXT_PREFIX
        ).joinToString(" ") { renderedText(it) }
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