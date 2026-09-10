package io.rippledown.kb.chat

import io.rippledown.chat.Conversation.Companion.CONDITION_TEXT_PARAMETER
import io.rippledown.chat.Conversation.Companion.NEW_VALUE_PARAMETER
import io.rippledown.chat.Conversation.Companion.SELECT_SUGGESTED_CONDITION
import io.rippledown.chat.Conversation.Companion.SUGGESTION_NUMBER_PARAMETER
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.chat.ReasonTransformation
import io.rippledown.kb.chat.SuggestedConditionsHandler.Companion.conditionTextOf
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.toJsonString

/**
 * Adds the suggested condition the user chose to the rule session.
 *
 * The user identifies a suggestion by its number in the list they were shown, and
 * that number is resolved here, against the list the system numbered. An editable
 * suggestion is completed here too: the model passes the user's new value and the
 * server substitutes it into the suggestion. So the model never transcribes a
 * condition — it got `Waves ≥ 1.5` and `Waves = 1.5` wrong often enough to make
 * that worth doing, and a mis-transcribed condition is silently a different rule.
 */
class SelectSuggestionHandler(
    private val case: RDRCase,
    private val ruleService: RuleService,
    private val suggestionsBuffer: SuggestionsBuffer = SuggestionsBuffer(),
) : FunctionCallHandler {
    override suspend fun handle(args: Map<String, Any?>): String {
        val number = intValueOf(args[SUGGESTION_NUMBER_PARAMETER])
        val conditionText = args[CONDITION_TEXT_PARAMETER]?.toString() ?: ""
        val newValue = args[NEW_VALUE_PARAMETER]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (number != null && suggestionsBuffer.numbered(number) == null) {
            return noSuchSuggestionNumberCorrection(number, suggestionsBuffer.shown.size)
        }
        val suggestion = number?.let { suggestionsBuffer.numbered(it) } ?: suggestionMatching(conditionText)
        val editableCondition = suggestion?.editableCondition()
        if (editableCondition != null) {
            if (newValue == null) return askForTheValueCorrection(suggestion.asText())
            val result = ruleService.conditionForEditedSuggestion(case, editableCondition, newValue)
            val condition = result.condition
                ?: return failure(suggestion.asText(), requireNotNull(result.errorMessage))
            return added(condition, condition.asText())
        }
        val identification = if (number != null) "suggestion $number" else "'$conditionText'"
        val condition = suggestion?.let { ruleService.conditionForSuggestionText(case, it.asText()) }
            ?: ruleService.conditionForSuggestionText(case, conditionTextOf(conditionText))
            ?: return failure(
                identification,
                "Could not find a matching non-editable suggestion for $identification. " +
                        "Please use the $TRANSFORM_REASON function instead."
            )
        return added(condition, identification)
    }

    private fun suggestionMatching(conditionText: String): SuggestedCondition? {
        val text = conditionTextOf(conditionText)
        return suggestionsBuffer.shown.firstOrNull { it.asText() == text }
    }

    private fun added(condition: Condition, identification: String): String {
        ruleService.addConditionToCurrentRuleSession(condition)
        val cornerstoneStatus = ruleService.cornerstoneStatus()
        ruleService.sendCornerstoneStatus()
        val transformation = ReasonTransformation(condition.id(), "Ok", cornerstoneStatus.toJsonString())
        val result = "'$identification' evaluation: ${transformation.toJsonString()}"
        return "$result\nCornerstone status: ${cornerstoneStatus.toJsonString()}"
    }

    private fun failure(identification: String, message: String) =
        "'$identification' evaluation: ${ReasonTransformation(message = message).toJsonString()}"

    /**
     * Gemini gives a number-typed argument as a [Number], but sends it as a
     * string often enough to be worth reading either.
     */
    private fun intValueOf(value: Any?): Int? = when (value) {
        null -> null
        is Number -> value.toInt()
        else -> value.toString().trim().toIntOrNull()
    }

    companion object {
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"

        fun noSuchSuggestionNumberCorrection(number: Int, numberOfSuggestions: Int) =
            "There is no suggestion numbered $number: the list shown to the user has " +
                    "$numberOfSuggestions suggestions. Tell the user that, and ask which suggestion they " +
                    "meant. Do NOT guess and do NOT transcribe a condition of your own."

        fun askForTheValueCorrection(suggestionText: String) =
            "The suggestion \"$suggestionText\" is editable, so it cannot be added until the user gives the " +
                    "value they want. Your VERY NEXT response MUST be a message only — no function call — that " +
                    "says \"you selected\" followed by the suggestion text, and asks \"What value would you " +
                    "like to use instead?\". When the user replies with a value, call " +
                    "$SELECT_SUGGESTED_CONDITION again with the SAME $SUGGESTION_NUMBER_PARAMETER and their " +
                    "value as $NEW_VALUE_PARAMETER."
    }
}
