package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KNOWLEDGE_BASES
import io.rippledown.constants.chat.OPEN_SUFFIX
import io.rippledown.kb.chat.KnowledgeBaseService

class ListKnowledgeBases : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val all = kbService.knowledgeBases()
        if (all.isEmpty()) return done(NO_KNOWLEDGE_BASES)
        val open = kbService.openKnowledgeBase()
        return done(all.joinToString("\n") { if (it == open) it.name + OPEN_SUFFIX else it.name })
    }
}
