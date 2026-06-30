package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.kb.chat.resolveCommentVariables
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class RemoveComment(val comment: String) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        if (ruleService.isRuleSessionActive()) {
            return ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        }
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")

        // The comment to remove must be converted to its internal form so it matches the conclusion
        // already stored for it (whose variable placeholders are held as VARIABLE_TOKEN); otherwise it
        // would not be found and a new, unmatched conclusion would be minted, which is not present in
        // the case's interpretation and so could not be removed.
        val (internalComment, _) = resolveCommentVariables(comment, emptyList(), ruleService)
        val cornerstoneStatus = ruleService.startRuleSessionToRemoveComment(sessionCase, internalComment)
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }

}