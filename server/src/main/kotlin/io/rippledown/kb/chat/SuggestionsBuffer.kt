package io.rippledown.kb.chat

import io.rippledown.model.condition.edit.SuggestedCondition

/**
 * Holds the most recent list of suggested conditions produced by the
 * [SuggestedConditionsHandler] so that the [ChatManager] can attach them
 * directly to the next [io.rippledown.model.chat.ChatResponse] without
 * requiring the LLM to echo the (potentially very long) list back to us.
 */
class SuggestionsBuffer {
    var suggestions: List<String>? = null

    /**
     * The suggestions of the list most recently shown, in the order they were
     * numbered for the user and for the model. Unlike [suggestions] this is not
     * consumed: it is how [SelectSuggestionHandler] resolves the number the user
     * gave back to the suggestion itself, so that neither the model nor the user
     * has to transcribe a condition's text.
     */
    var shown: List<SuggestedCondition> = emptyList()

    fun consume(): List<String>? {
        val s = suggestions
        suggestions = null
        return s
    }

    /**
     * The suggestion the user identified by [number], numbered from 1 as it was
     * displayed, or null if there is no such suggestion.
     */
    fun numbered(number: Int): SuggestedCondition? = shown.getOrNull(number - 1)
}
