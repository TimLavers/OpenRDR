package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Shows the user a description of the rule that would be removed if they
 * confirmed the undo, and asks them to reply "yes" to proceed. Acts as the
 * first step of a two-turn confirmation flow whose second step is
 * [UndoLastRule].
 *
 * If there is no rule available to undo, the user is told so and no
 * confirmation is requested.
 */
class ShowLastRuleForUndo : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        val description = ruleService.descriptionOfMostRecentRule()
        if (!description.canRemove) {
            return ChatResponse(description.description)
        }
        return ChatResponse(confirmRemovalMessage(description.description))
    }

    companion object {
        fun confirmRemovalMessage(ruleDescription: String): String =
            "The last rule was: \"$ruleDescription\".\n\n" +
                    "Please confirm you wish to remove it."
    }
}
