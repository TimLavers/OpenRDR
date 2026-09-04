package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbDescriptionUpdatedMessage
import io.rippledown.kb.chat.KnowledgeBaseService

data class SetKnowledgeBaseDescription(val description: String) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val open = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        kbService.setDescription(description)
        return done(kbDescriptionUpdatedMessage(open.name))
    }
}
