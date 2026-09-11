package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.kbCopiedFromDemonstrationMessage
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB

data class CopyDemonstrationKnowledgeBase(val sample: SampleKB, val kbName: String) : KbManagementAction {
    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome =
        outcomeForNewKbName(kbService, kbName) { service, name ->
            val created = service.createFromSample(name, sample)
            ChatResponse(kbCopiedFromDemonstrationMessage(created.name, sample.title()))
        }
}
