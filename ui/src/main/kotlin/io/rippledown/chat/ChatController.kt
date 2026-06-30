package io.rippledown.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse
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
    voiceRecognitionService: VoiceRecognition? = null,
    modifier: Modifier = Modifier
) {
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
    var sendIsEnabled: Boolean by remember { mutableStateOf(true) }

    handler.onBotMessageReceived = { response ->
        val lastBotMessage = chatHistory.lastOrNull()
        val isDuplicate = lastBotMessage is BotMessage && lastBotMessage.text == response.text
        if (!isDuplicate) {
            val additions = buildList {
                if (response.text.isEmpty()) {
                    add(BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE))
                } else {
                    // The tip is shown ahead of the bot's message so it lands right after the
                    // user's comment, before the suggestions are presented.
                    response.tip?.let { add(TipMessage(it)) }
                    add(BotMessage(response.text))
                    if (response.suggestions.isNotEmpty()) add(SuggestionListMessage(response.suggestions))
                }
            }
            chatHistory = chatHistory + additions
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