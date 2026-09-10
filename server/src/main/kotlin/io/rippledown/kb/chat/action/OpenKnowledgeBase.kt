package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.confirmKbOpenMessage
import io.rippledown.constants.chat.kbAmbiguousMessage
import io.rippledown.constants.chat.kbNotFoundMessage
import io.rippledown.constants.chat.kbOpenedMessage
import io.rippledown.kb.KbResolution
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse

data class OpenKnowledgeBase(val kbName: String) : KbManagementAction {

    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome =
        when (val resolution = kbService.resolve(kbName)) {
            is KbResolution.Exact -> KbManagementOutcome.Done(open(kbService, resolution.kbInfo))
            is KbResolution.Partial -> KbManagementOutcome.Ask(confirmKbOpenMessage(resolution.kbInfo.name)) {
                open(it, resolution.kbInfo)
            }

            is KbResolution.Ambiguous -> done(kbAmbiguousMessage(resolution.name, resolution.candidates))
            is KbResolution.NotFound -> done(kbNotFoundMessage(resolution.name, resolution.available))
        }

    private suspend fun open(kbService: KnowledgeBaseService, kbInfo: KBInfo): ChatResponse {
        kbService.open(kbInfo)
        return ChatResponse(kbOpenedMessage(kbInfo.name))
    }
}
