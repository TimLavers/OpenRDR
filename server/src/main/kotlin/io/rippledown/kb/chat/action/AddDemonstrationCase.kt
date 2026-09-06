package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.demoCaseAddedMessage
import io.rippledown.kb.chat.KnowledgeBaseService

/**
 * Adds a demonstration case to the open knowledge base. The case arrives at
 * the client as any other case does, so the client selects it and restarts the
 * conversation;
 */
class AddDemonstrationCase : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        val case = kbService.addDemonstrationCase()
        return done(demoCaseAddedMessage(case.name))
    }
}
