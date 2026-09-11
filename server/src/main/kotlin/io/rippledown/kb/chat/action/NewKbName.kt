package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.BLANK_NAME_MESSAGE
import io.rippledown.constants.chat.confirmKbCreateMessage
import io.rippledown.constants.chat.kbAlreadyExistsMessage
import io.rippledown.constants.chat.kbNameReservedMessage
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse

suspend fun outcomeForNewKbName(
    kbService: KnowledgeBaseService,
    rawName: String,
    create: suspend (KnowledgeBaseService, String) -> ChatResponse
): KbManagementOutcome {
    val name = rawName.trim()
    if (name.isEmpty()) return done(BLANK_NAME_MESSAGE)
    if (kbService.isDemonstrationTitle(name)) return done(kbNameReservedMessage(name))
    val existing = kbService.resolve(name)
    if (existing is KbResolution.Exact) return done(kbAlreadyExistsMessage(existing.kbInfo.name))
    val nearDuplicate = kbService.nearDuplicateOf(name)
    if (nearDuplicate != null) {
        return KbManagementOutcome.Ask(confirmKbCreateMessage(name, nearDuplicate.name)) { create(it, name) }
    }
    return KbManagementOutcome.Done(create(kbService, name))
}
