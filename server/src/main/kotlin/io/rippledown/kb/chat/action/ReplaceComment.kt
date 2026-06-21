package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.CommentVariable
import io.rippledown.model.VARIABLE_TOKEN
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class ReplaceComment(
    val comment: String,
    val replacementComment: String,
    val variables: List<CommentVariable> = emptyList()
) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        if (ruleService.isRuleSessionActive()) {
            return ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        }
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")

        // Convert {attributeName} placeholders to the internal VARIABLE_TOKEN (${}) in the replacement comment
        val internalReplacementComment =
            replacementComment.replace(Regex("\\{[^}]*\\}"), Regex.escapeReplacement(VARIABLE_TOKEN))

        val cornerstoneStatus =
            ruleService.startRuleSessionToReplaceComment(sessionCase, comment, internalReplacementComment, variables)
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }
}