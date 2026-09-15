package io.rippledown.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest
import io.rippledown.voice.VoiceRecognition

interface ChatControllerHandler {
    fun sendUserMessage(message: String)
    var onBotMessageReceived: (response: ChatResponse) -> Unit
}

/**
 * @author Cascade AI
 */
@Composable
fun ChatController(
    id: Long = -1L,
    handler: ChatControllerHandler,
    conversationStarted: Boolean = true,
    voiceRecognitionService: VoiceRecognition? = null,
    modifier: Modifier = Modifier,
    state: ChatState = remember { ChatState() },
    fileTransferInProgress: Boolean = false,
    onFileDialogRequested: (KbFileDialogRequest) -> Unit = {}
) {
    handler.onBotMessageReceived = { response ->
        state.receive(response, onFileDialogRequested)
    }

    ChatPanel(
        id,
        conversationStarted && !state.awaitingResponse && !fileTransferInProgress,
        state.history,
        onMessageSent = { userMessage ->
            state.userMessage(userMessage)
        handler.sendUserMessage(userMessage.text)
    }, voiceRecognitionService, modifier)
}
