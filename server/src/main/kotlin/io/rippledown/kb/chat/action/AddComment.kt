package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ChatCommentVariable
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.kb.chat.resolveCommentVariables
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Starts a rule session to add a comment. The server auto-names the
 * comment attribute (C1, C2, …); the user can rename it later. See step 14
 * of documentation/design/repeat_inferencing.md.
 */
data class AddComment(
    val comment: String,
    val variables: List<ChatCommentVariable> = emptyList()
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

        // Convert {attributeName} placeholders to the internal VARIABLE_TOKEN (${}), aligning the
        // variables to the placeholders actually present so a comment with no placeholders carries none.
        val (internalComment, resolvedVariables) = resolveCommentVariables(comment, variables, ruleService)

        val cornerstoneStatus =
            ruleService.startRuleSessionToAddComment(sessionCase, internalComment, resolvedVariables)
        ruleService.sendCornerstoneStatus()
        val response = modelResponder.response(cornerstoneStatus.summary())
        return response.withCommentName(ruleService.nameOfCommentAttributeInSession())
    }
}