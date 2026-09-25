package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.*
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Starts a rule session to add a comment. The server auto-names the
 * comment attribute (C1, C2, …); the user can rename it later.
 */
data class AddComment(
    val comment: String,
    val variables: List<ChatCommentVariable> = emptyList(),
    val reasons: List<String> = emptyList(),
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
        val response = respondAfterSessionStart(ruleService, sessionCase, cornerstoneStatus, reasons, modelResponder)
        return response.withCommentName(ruleService.nameOfCommentAttributeInSession())
    }
}