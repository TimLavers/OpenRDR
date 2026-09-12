package io.rippledown.kb.chat

import io.rippledown.chat.FunctionCallHandler
import io.rippledown.constants.chat.*
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Applies the server's presentation policies after a chat action has run: combines suggested conditions,
 * supplies missing suggestions when a rule session starts, and excludes conditions already in the rule.
 * Keeps the comment-variable tip to once per conversation; [reset] starts that lifecycle again.
 * These policies live here so [ChatManager] can focus on routing replies and executing actions.
 */
class ChatResponseEnricher(
    private val ruleService: RuleService?,
    private val suggestionsBuffer: SuggestionsBuffer,
    private val suggestedConditionsHandler: FunctionCallHandler? = null
) {
    private var commentVariableTipResolved = false

    fun reset() {
        commentVariableTipResolved = false
    }

    suspend fun enrich(
        actionComment: ActionComment,
        chatResponse: ChatResponse,
        currentCase: ViewableCase?
    ): ChatResponse {
        val tip = commentVariableTipFor(actionComment, chatResponse, currentCase)
        val bufferedSuggestions = suggestionsBuffer.consume()
        val response = when {
            bufferedSuggestions != null -> chatResponse.copy(suggestions = bufferedSuggestions, tip = tip)
            !actionComment.suggestions.isNullOrEmpty() -> chatResponse.copy(
                suggestions = actionComment.suggestions,
                tip = tip
            )

            else -> chatResponse.copy(tip = tip ?: chatResponse.tip)
        }
        return withoutConditionsAlreadyInTheRule(ensureSuggestionsAfterStartingRuleSession(actionComment, response))
    }

    /**
     * Drops any suggestion that is already a condition of the rule being built.
     *
     * [SuggestedConditionsHandler] does exclude the conditions added so far, but it does so when the
     * *model* calls it, which can be earlier in the same turn than the condition is added. The model
     * sometimes calls {@code getSuggestedConditions} and then {@code selectSuggestion} in the one turn,
     * in which case the buffered list was computed before the selected condition existed and would
     * re-offer the very condition the user just chose. Filtering here, as the response is assembled,
     * uses the session's conditions as they finally stand, so it is immune to the order of the model's
     * function calls. It also covers the list the model may echo back in its own JSON, which nothing
     * else filters.
     */
    private fun withoutConditionsAlreadyInTheRule(response: ChatResponse): ChatResponse {
        if (response.suggestions.isEmpty()) return response
        val alreadyUsed = ruleService?.currentRuleSessionConditionTexts() ?: return response
        if (alreadyUsed.isEmpty()) return response
        val remaining = response.suggestions.filterNot {
            SuggestedConditionsHandler.conditionTextOf(it) in alreadyUsed
        }
        return if (remaining.size == response.suggestions.size) response else response.copy(suggestions = remaining)
    }

    /**
     * Guarantee that suggested conditions accompany the response when an action has just started a
     * rule session. The model is instructed to call {@code getSuggestedConditions} immediately after
     * the session starts, but some models (e.g. Gemini flash-lite) instead go straight to asking the
     * user for a reason, leaving the user with a question and no suggestions - which stalls the flow.
     * In that case, populate the suggestions deterministically.
     */
    private suspend fun ensureSuggestionsAfterStartingRuleSession(
        actionComment: ActionComment,
        response: ChatResponse
    ): ChatResponse {
        if (actionComment.action !in SESSION_STARTING_ACTIONS) return response
        if (response.suggestions.isNotEmpty()) return response
        val handler = suggestedConditionsHandler ?: return response
        if (!isRuleSessionActive()) return response
        handler.handle(emptyMap())
        val suggestions = suggestionsBuffer.consume()
        return if (suggestions.isNullOrEmpty()) response else response.copy(suggestions = suggestions)
    }

    /**
     * The first time the user adds a comment in a session, return a short one-line tip explaining that a
     * comment can include a case value by wrapping an attribute name in braces (e.g. {Glucose}). The tip
     * is delivered on the [ChatResponse.tip] channel so the UI can render it distinctly. It is shown at
     * most once per session and is suppressed when the user has already used the facility (i.e. the comment
     * already contains a placeholder) or when the add was rejected because a rule session was already active.
     */
    private fun commentVariableTipFor(
        actionComment: ActionComment,
        chatResponse: ChatResponse,
        currentCase: ViewableCase?
    ): String? {
        if (commentVariableTipResolved) return null
        if (ruleService == null) return null
        if (actionComment.action != ADD_COMMENT) return null
        if (chatResponse.text == RULE_SESSION_ALREADY_ACTIVE_ERROR) return null
        val comment = actionComment.comment ?: return null
        if (comment.contains("{")) {
            // The user has already used the facility, so they know about it: never offer the tip this
            // session, even for later comments that don't use a variable.
            commentVariableTipResolved = true
            return null
        }
        commentVariableTipResolved = true
        // Use the first attribute of the displayed case as the example, falling back to a generic name
        // if the case has no attributes, so the tip is concrete and relevant to what the user is seeing.
        val exampleAttribute = currentCase?.attributes()?.firstOrNull()?.name ?: DEFAULT_TIP_EXAMPLE_ATTRIBUTE
        return commentVariableTip(exampleAttribute)
    }


    private fun isRuleSessionActive() = ruleService?.isRuleSessionActive() == true

    companion object {
        const val DEFAULT_TIP_EXAMPLE_ATTRIBUTE = "TSH"
        val SESSION_STARTING_ACTIONS = setOf(
            ADD_COMMENT, REMOVE_COMMENT, REPLACE_COMMENT,
            ASSIGN_DERIVED_VALUE, REMOVE_DERIVED_VALUE, REPLACE_DERIVED_VALUE,
        )

        fun commentVariableTip(exampleAttributeName: String) =
            "Tip: you can include a case value in a comment by wrapping an attribute name in " +
                    "$COMMENT_VARIABLE_TIP_KEYWORD, e.g. {$exampleAttributeName}."
    }
}
