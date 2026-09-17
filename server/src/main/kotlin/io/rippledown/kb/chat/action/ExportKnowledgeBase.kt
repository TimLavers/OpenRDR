package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.constants.chat.kbExportFileDialogMessage
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest
import java.util.*

class ExportKnowledgeBase : KbManagementAction {
    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome {
        val kbInfo = kbService.openKnowledgeBase() ?: return done(NO_KB_OPEN_MESSAGE)
        return KbManagementOutcome.Done(
            ChatResponse(
                kbExportFileDialogMessage(kbInfo.name),
                kbFileDialogRequest = KbFileDialogRequest.Export(UUID.randomUUID().toString(), kbInfo)
            )
        )
    }
}
