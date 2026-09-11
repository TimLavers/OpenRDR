package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.kbCreatedMessage
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse

data class CreateKnowledgeBase(val kbName: String) : KbManagementAction {

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome =
        outcomeForNewKbName(kbService, kbName) { service, name -> create(service, name) }

    private suspend fun create(kbService: KnowledgeBaseService, name: String): ChatResponse {
        val created = kbService.create(name)
        return ChatResponse(kbCreatedMessage(created.name))
    }
}
