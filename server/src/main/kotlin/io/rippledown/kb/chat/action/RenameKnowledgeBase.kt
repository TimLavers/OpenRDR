package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.KB_NAME_CANNOT_BE_BLANK
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbAlreadyExistsMessage
import io.rippledown.constants.chat.kbRenamedMessage
import io.rippledown.kb.chat.KnowledgeBaseService

data class RenameKnowledgeBase(val newName: String) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val open = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        if (newName.isBlank()) return done(KB_NAME_CANNOT_BE_BLANK)
        val renamed = try {
            kbService.rename(newName)
        } catch (_: IllegalArgumentException) {
            return done(kbAlreadyExistsMessage(newName))
        }
        return done(kbRenamedMessage(open.name, renamed.name))
    }
}
