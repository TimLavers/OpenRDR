package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse

/**
 * Never deletes on its own turn: deletion is irreversible, so even an exact
 * match is put to the user first.
 */
data class DeleteKnowledgeBase(val kbName: String? = null) : KbManagementAction {

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val name = kbName ?: kbService.openKnowledgeBase()?.name ?: return done(NO_KB_OPEN_MESSAGE)
        return when (val resolution = kbService.resolve(name)) {
            is KbResolution.Exact -> askToDelete(resolution.kbInfo)
            is KbResolution.Partial -> askToDelete(resolution.kbInfo)
            is KbResolution.Ambiguous -> done(kbAmbiguousMessage(resolution.name, resolution.candidates))
            is KbResolution.NotFound -> done(kbNotFoundMessage(resolution.name, resolution.available))
        }
    }

    private fun askToDelete(kbInfo: KBInfo) = KbManagementOutcome.Ask(confirmKbDeletionMessage(kbInfo.name)) {
        it.delete(kbInfo)
        ChatResponse(kbDeletedMessage(kbInfo.name))
    }
}
