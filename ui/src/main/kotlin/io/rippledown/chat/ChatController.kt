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
    conversationStarted: Boolean = true,
    voiceRecognitionService: VoiceRecognition? = null,
    modifier: Modifier = Modifier
) {
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
    var sendIsEnabled: Boolean by remember { mutableStateOf(true) }

    handler.onBotMessageReceived = { response ->
        val lastBotMessage = chatHistory.lastOrNull()
        val isDuplicate = when (lastBotMessage) {
            is KbChoiceListMessage -> lastBotMessage.text == response.text && lastBotMessage.listing == response.kbListing
            is BotMessage -> lastBotMessage.text == response.text && response.kbListing == null
            else -> false
        }
        if (!isDuplicate) {
            val additions = buildList {
                if (response.text.isEmpty()) {
                    add(BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE))
                } else {
                    // The tip is shown ahead of the bot's message so it lands right after the
                    // user's comment, before the suggestions are presented.
                    response.tip?.let { add(TipMessage(it)) }
                    val listing = response.kbListing
                    if (listing == null) add(BotMessage(response.text))
                    else add(KbChoiceListMessage(response.text, listing))
                    if (response.suggestions.isNotEmpty()) add(SuggestionListMessage(response.suggestions))
                }
            }
            chatHistory = chatHistory + additions
        }
        sendIsEnabled = true
    }

    ChatPanel(id, conversationStarted && sendIsEnabled, chatHistory, onMessageSent = { userMessage ->
        if (userMessage.text.isNotEmpty()) {
            chatHistory = chatHistory + userMessage
        }
        sendIsEnabled = false
        handler.sendUserMessage(userMessage.text)
    }, voiceRecognitionService, modifier)
}
