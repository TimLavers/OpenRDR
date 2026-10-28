package io.rippledown.kb.chat.action

import io.rippledown.kb.ai.AiDraftPreview
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.kb.ai.toView
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.respondAfterSessionStart
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.AiBooleanExample
import kotlinx.coroutines.CancellationException

open class ProposeAiDerivedAttribute(
    val attributeName: String,
    val sourceName: String,
    val instructions: String,
    val examples: List<AiBooleanExample> = emptyList(),
    val reasons: List<String> = emptyList(),
    private val edit: Boolean = false
) : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse =
        authoringResponse {
            val draft = ruleService.proposeAiDefinition(attributeName, sourceName, instructions, examples,
                currentCase?.id, reasons, edit)
            val view = try {
                ruleService.previewAiDefinition(draft.id, currentCase?.case)
            } catch (e: InterpretationIncompleteException) {
                draft.toView(AiDraftPreview(
                    message = "Could not evaluate the preview: ${e.details.reason}. You can retry this draft."))
            }
            ChatResponse("${draft.description()}\n\n${view.previewText}\n\n${view.prompt}", draft = view)
        }
}

class EditAiDerivedAttributeDefinition(
    attributeName: String, sourceName: String, instructions: String,
    examples: List<AiBooleanExample> = emptyList()
) : ProposeAiDerivedAttribute(attributeName, sourceName, instructions, examples, edit = true)

class AcceptAiDerivedAttribute(val draftId: String) : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse =
        authoringResponse {
            val accepted = ruleService.acceptAiDefinition(draftId, currentCase?.case)
            val status = accepted.cornerstoneStatus
            if (status == null) ChatResponse(accepted.message)
            else respondAfterSessionStart(ruleService, requireNotNull(currentCase), status, accepted.reasons, modelResponder)
        }
}

class PreviewAiDefinition(val draftId: String) : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse =
        authoringResponse {
            val draft = checkNotNull(ruleService.currentAiDefinitionDraft()?.takeIf { it.id == draftId }) {
                "This draft is no longer current."
            }
            val view = ruleService.previewAiDefinition(draftId, currentCase?.case)
            ChatResponse("${draft.description()}\n\n${view.previewText}\n\n${view.prompt}", draft = view)
        }
}

class CancelAiDefinitionDraft : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse {
        ruleService.cancelAiDefinitionDraft()
        return ChatResponse("The AI definition draft has been cancelled.")
    }
}

/**
 * The user's explicit request to try a failed evaluation again now, bypassing
 * the failure cooldown. The current draft is unchanged, so accepting it
 * commits the same revision the user approved.
 */
class RetryAiEvaluation : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse =
        authoringResponse {
            val draft = ruleService.currentAiDefinitionDraft()
            if (draft == null) {
                val selected = requireNotNull(currentCase) { "Select a case before retrying evaluation." }
                ChatResponse(ruleService.retryAiEvaluation(selected.case))
            } else {
                val view = ruleService.previewAiDefinition(draft.id, currentCase?.case, retryAfterFailure = true)
                ChatResponse("${draft.description()}\n\n${view.previewText}\n\n${view.prompt}", draft = view)
            }
        }
}

/**
 * The value and evidence of a committed AI definition on the current case,
 * with the approved criteria. Explaining never revises the definition and,
 * for an already evaluated case, makes no provider call.
 */
class ExplainAiEvaluation(val attributeName: String) : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse =
        authoringResponse {
            val view = ruleService.explainAiEvaluation(attributeName, currentCase?.case)
            ChatResponse(view.explanationText, draft = view)
        }
}

private suspend fun authoringResponse(action: suspend () -> ChatResponse): ChatResponse = try {
    action()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (e: InterpretationIncompleteException) {
    ChatResponse("Could not evaluate: ${e.details.reason} You can retry.")
} catch (e: IllegalArgumentException) {
    ChatResponse(e.message ?: "The definition is invalid.")
} catch (e: IllegalStateException) {
    ChatResponse(e.message ?: "The definition could not be accepted.")
}
