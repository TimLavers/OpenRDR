package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.commentNamedMessage
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

interface ChatAction {
    suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse

    companion object {
        const val RULE_SESSION_ALREADY_ACTIVE_ERROR =
            "Please finish or cancel the current rule before starting a new one."
    }
}

/**
 * This response, told that the comment just accepted is named [name] and can
 * be renamed. The message is added by the server rather than left to the
 * model, so that the name is always stated, exactly once, and correctly. See
 * step 14 of documentation/design/repeat_inferencing.md.
 */
fun ChatResponse.withCommentName(name: String?) =
    if (name.isNullOrBlank()) this else copy(text = "${commentNamedMessage(name)}\n\n$text")