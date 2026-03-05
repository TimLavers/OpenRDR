package io.rippledown.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse

interface ChatControllerHandler {
    fun sendUserMessage(message: String)
    var onBotMessageReceived: (response: ChatResponse) -> Unit
}

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
        val suggestions = response.suggestions.ifEmpty { extractSuggestionsFromText(response.text) }
        chatHistory = if (response.text.isEmpty()) {
            chatHistory + BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE)
        } else if (suggestions.isNotEmpty()) {
            val textWithoutNumberedItems = response.text.lines()
                .filterNot { it.matches(Regex("^\\s*\\d+\\.\\s+.*")) }
                .joinToString("\n")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
            chatHistory + BotMessage(textWithoutNumberedItems) + SuggestionListMessage("", suggestions)
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

private val NUMBERED_ITEM_PATTERN = Regex("^\\s*\\d+\\.\\s+(.+)$")

fun extractSuggestionsFromText(text: String): List<String> {
    return text.lines()
        .filter { it.matches(NUMBERED_ITEM_PATTERN) }
        .map { NUMBERED_ITEM_PATTERN.find(it)!!.groupValues[1].trim() }
}