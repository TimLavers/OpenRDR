package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.demoCaseAddedMessage
import io.rippledown.kb.chat.DemonstrationCase
import io.rippledown.kb.chat.KnowledgeBaseService

/**
 * Adds a demonstration case to the open knowledge base. The case arrives at
 * the client as any other case does, so the client selects it and restarts the
 * conversation; nothing changes on the server side of the context here.
 */
data class AddDemonstrationCase(val kind: String) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        val demonstrationCase = DemonstrationCase.entries.firstOrNull { it.name.equals(kind.trim(), ignoreCase = true) }
            ?: return done(unknownKindMessage(kind))
        val case = kbService.addDemonstrationCase(demonstrationCase)
        return done(demoCaseAddedMessage(case.name))
    }

    companion object {
        fun unknownKindMessage(kind: String) =
            "I don't have a \"$kind\" demonstration case. The kinds are: " +
                    DemonstrationCase.entries.joinToString(", ") { it.name.lowercase() } + "."
    }
}
