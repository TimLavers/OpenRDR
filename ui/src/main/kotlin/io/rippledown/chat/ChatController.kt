package io.rippledown.chat

import androidx.compose.runtime.*
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE

interface ChatControllerHandler {
    fun sendUserMessage(message: String)
    var onBotMessageReceived: (message: String) -> Unit
}

@Composable
fun ChatController(handler: ChatControllerHandler) {
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(emptyList()) }
    var sendIsEnabled: Boolean by remember { mutableStateOf(true) }

    handler.onBotMessageReceived = { message ->
        val botMessage = if (message.isEmpty()) {
            BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE)
        } else {
            BotMessage(message)
        }
        chatHistory = chatHistory + botMessage
        sendIsEnabled = true
    }

    ChatPanel(sendIsEnabled, chatHistory, onMessageSent = { userMessage ->
        if (userMessage.text.isNotEmpty()) {
            chatHistory = chatHistory + userMessage
        }
        sendIsEnabled = false
        handler.sendUserMessage(userMessage.text)
    })
}