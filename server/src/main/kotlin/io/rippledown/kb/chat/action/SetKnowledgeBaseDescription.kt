package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse

data class SetKnowledgeBaseDescription(val description: String, val kbName: String? = null) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        if (kbName == null) {
            val open = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
            return KbManagementOutcome.Done(set(kbService, open))
        }
        return when (val resolution = kbService.resolve(kbName)) {
            is KbResolution.Exact -> KbManagementOutcome.Done(set(kbService, resolution.kbInfo))
            is KbResolution.Partial -> KbManagementOutcome.Ask(confirmKbOpenMessage(resolution.kbInfo.name)) {
                set(it, resolution.kbInfo)
            }

            is KbResolution.Demonstration -> done(cannotDescribeDemonstrationMessage(resolution.sample.title()))
            is KbResolution.Ambiguous -> done(kbAmbiguousMessage(resolution.name, resolution.candidates))
            is KbResolution.NotFound -> done(
                kbNotFoundMessage(resolution.name, resolution.available, resolution.demonstrations)
            )
        }
    }

    private fun set(kbService: KnowledgeBaseService, kbInfo: KBInfo): ChatResponse {
        kbService.setDescription(kbInfo, description)
        return ChatResponse(kbDescriptionUpdatedMessage(kbInfo.name))
    }
}
