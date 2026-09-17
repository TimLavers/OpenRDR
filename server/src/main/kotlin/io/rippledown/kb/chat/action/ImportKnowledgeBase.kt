package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.KB_IMPORT_FILE_DIALOG_MESSAGE
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest
import java.util.*

class ImportKnowledgeBase : KbManagementAction {
    override suspend fun doIt(kbService: KnowledgeBaseService) = KbManagementOutcome.Done(
        ChatResponse(
            KB_IMPORT_FILE_DIALOG_MESSAGE,
            kbFileDialogRequest = KbFileDialogRequest.Import(UUID.randomUUID().toString())
        )
    )
}
