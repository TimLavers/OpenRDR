package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbClosedMessage
import io.rippledown.kb.chat.KnowledgeBaseService

class CloseKnowledgeBase : KbManagementAction {

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val open = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        kbService.close()
        return done(kbClosedMessage(open.name))
    }
}
