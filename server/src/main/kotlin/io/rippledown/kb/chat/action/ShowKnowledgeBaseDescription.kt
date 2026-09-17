package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo

data class ShowKnowledgeBaseDescription(val kbName: String? = null) : KbManagementAction {
    override val changesContext = false

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val open = kbService.openKnowledgeBase()
        if (kbName == null) {
            return if (open == null) done(NO_KB_OPEN_MESSAGE) else describe(kbService, open, open)
        }
        return when (val resolution = kbService.resolve(kbName)) {
            is KbResolution.Exact -> describe(kbService, resolution.kbInfo, open)
            is KbResolution.Partial -> describe(kbService, resolution.kbInfo, open)
            is KbResolution.Demonstration ->
                done(kbDescriptionOfMessage(resolution.sample.title(), resolution.sample.description()))

            is KbResolution.Ambiguous -> done(kbAmbiguousMessage(resolution.name, resolution.candidates))
            is KbResolution.NotFound -> done(
                kbNotFoundMessage(resolution.name, resolution.available, resolution.demonstrations)
            )
        }
    }

    private fun describe(kbService: KnowledgeBaseService, kbInfo: KBInfo, open: KBInfo?): KbManagementOutcome {
        val description = kbService.description(kbInfo)
        return done(
            when {
                description.isBlank() -> kbHasNoDescriptionMessage(kbInfo.name)
                kbInfo == open -> description
                else -> kbDescriptionOfMessage(kbInfo.name, description)
            }
        )
    }
}
