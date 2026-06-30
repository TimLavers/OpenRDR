package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ChatCommentVariable
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.kb.chat.resolveCommentVariables
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class ReplaceComment(
    val comment: String,
    val replacementComment: String,
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

        // Convert {attributeName} placeholders to the internal VARIABLE_TOKEN (${}) in the replacement
        // comment, aligning the variables to the placeholders actually present so a replacement with no
        // placeholders carries none.
        val (internalReplacementComment, resolvedVariables) =
            resolveCommentVariables(replacementComment, variables, ruleService)

        // The to-be-replaced comment must also be converted to its internal form so it matches the
        // conclusion already stored for it (whose variable placeholders are held as VARIABLE_TOKEN);
        // otherwise it would not be found and a new, unmatched conclusion would be minted. Its own
        // variables come from the existing conclusion, so none need to be derived here.
        val (internalReplacedComment, _) = resolveCommentVariables(comment, emptyList(), ruleService)

        val cornerstoneStatus = ruleService.startRuleSessionToReplaceComment(
            sessionCase,
            internalReplacedComment,
            internalReplacementComment,
            resolvedVariables
        )
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }
}