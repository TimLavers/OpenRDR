package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbHasNoDescriptionMessage
import io.rippledown.kb.chat.KnowledgeBaseService

class ShowKnowledgeBaseDescription : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val open = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        val description = kbService.description()
        return done(description.ifBlank { kbHasNoDescriptionMessage(open.name) })
    }
}
