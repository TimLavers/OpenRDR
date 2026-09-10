package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.confirmKbCreateMessage
import io.rippledown.constants.chat.kbAlreadyExistsMessage
import io.rippledown.constants.chat.kbCreatedMessage
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse

data class CreateKnowledgeBase(val kbName: String) : KbManagementAction {

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val name = kbName.trim()
        if (name.isEmpty()) return done(BLANK_NAME_MESSAGE)
        val existing = kbService.resolve(name)
        if (existing is KbResolution.Exact) return done(kbAlreadyExistsMessage(existing.kbInfo.name))
        val nearDuplicate = kbService.nearDuplicateOf(name)
        if (nearDuplicate != null) {
            return KbManagementOutcome.Ask(confirmKbCreateMessage(name, nearDuplicate.name)) { create(it, name) }
        }
        return KbManagementOutcome.Done(create(kbService, name))
    }

    private suspend fun create(kbService: KnowledgeBaseService, name: String): ChatResponse {
        val created = kbService.create(name)
        return ChatResponse(kbCreatedMessage(created.name))
    }

    companion object {
        const val BLANK_NAME_MESSAGE = "Please give the new knowledge base a name."
    }
}
