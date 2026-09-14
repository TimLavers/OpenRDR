package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.DEMONSTRATION_KNOWLEDGE_BASES_HEADING
import io.rippledown.constants.chat.NO_KNOWLEDGE_BASES_OF_YOUR_OWN
import io.rippledown.constants.chat.OPEN_SUFFIX
import io.rippledown.constants.chat.YOUR_KNOWLEDGE_BASES
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KnowledgeBaseListing
import io.rippledown.model.chat.summaryOf

class ListKnowledgeBases : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val all = kbService.knowledgeBases()
        val open = kbService.openKnowledgeBase()
        val samples = kbService.demonstrations().sortedBy { it.title() }
        val demonstrations = samples.map { it.title() }
        val storedSection = if (all.isEmpty()) NO_KNOWLEDGE_BASES_OF_YOUR_OWN
        else "$YOUR_KNOWLEDGE_BASES\n" +
                all.joinToString("\n") { if (it == open) it.name + OPEN_SUFFIX else it.name }
        val text = "$storedSection\n\n$DEMONSTRATION_KNOWLEDGE_BASES_HEADING\n" +
                demonstrations.joinToString("\n")
        val descriptions = all.associate { it.name to summaryOf(kbService.description(it)) }
            .filterValues { it.isNotBlank() } +
                samples.associate { it.title() to it.description() }
        val listing = KnowledgeBaseListing(all.map { it.name }, demonstrations, open?.name, descriptions)
        return KbManagementOutcome.Done(ChatResponse(text, kbListing = listing))
    }
}
