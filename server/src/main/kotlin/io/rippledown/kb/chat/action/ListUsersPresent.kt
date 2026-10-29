package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.kb.chat.KnowledgeBaseService

class ListUsersPresent : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val kb = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        val users = kbService.usersPresent()
        if (users.isEmpty()) return done("Nobody else is working on ${kb.name} right now.")
        val names = users.joinToString(", ") { if (it.editing) "${it.userId} (building a rule)" else it.userId }
        return done("Also working on ${kb.name}: $names.")
    }
}
