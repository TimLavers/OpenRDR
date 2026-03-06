package io.rippledown.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse

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
    voiceRecognitionService: VoiceRecognition? = null,
    modifier: Modifier = Modifier
) {
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
    var sendIsEnabled: Boolean by remember { mutableStateOf(true) }

    handler.onBotMessageReceived = { response ->
        chatHistory = if (response.text.isEmpty()) {
            chatHistory + BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE)
        } else if (response.suggestions.isNotEmpty()) {
            chatHistory + BotMessage(response.text) + SuggestionListMessage("", response.suggestions)
        } else {
            chatHistory + BotMessage(response.text)
        }
        sendIsEnabled = true
    }

    ChatPanel(id, sendIsEnabled, chatHistory, onMessageSent = { userMessage ->
        if (userMessage.text.isNotEmpty()) {
            chatHistory = chatHistory + userMessage
        }
        sendIsEnabled = false
        handler.sendUserMessage(userMessage.text)
    }, voiceRecognitionService, modifier)
}