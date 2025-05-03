package io.rippledown.chat

import androidx.compose.runtime.*
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE

interface ChatControllerHandler {
    fun sendUserMessage(message: String)
    var onBotMessageReceived: (message: String) -> Unit
}

@Composable
fun ChatController(handler: ChatControllerHandler) {
    val initialBotMessage = BotMessage(CHAT_BOT_INITIAL_MESSAGE)
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(listOf(initialBotMessage)) }

    handler.onBotMessageReceived = { message ->
        val botMessage = if (message.isEmpty()) {
            BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE)
        } else {
            BotMessage(message)
        }
        chatHistory = chatHistory + botMessage
    }

    ChatPanel(chatHistory, onMessageSent = { userMessage ->
        if (userMessage.text.isNotEmpty()) {
            chatHistory = chatHistory + userMessage
        }
        handler.sendUserMessage(userMessage.text)
    })
}