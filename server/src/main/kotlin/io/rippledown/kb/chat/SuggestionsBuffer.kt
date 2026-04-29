package io.rippledown.kb.chat

/**
 * Holds the most recent list of suggested conditions produced by the
 * [SuggestedConditionsHandler] so that the [ChatManager] can attach them
 * directly to the next [io.rippledown.model.chat.ChatResponse] without
 * requiring the LLM to echo the (potentially very long) list back to us.
 */
class SuggestionsBuffer {
    var suggestions: List<String>? = null

    fun consume(): List<String>? {
        val s = suggestions
        suggestions = null
        return s
    }
}
