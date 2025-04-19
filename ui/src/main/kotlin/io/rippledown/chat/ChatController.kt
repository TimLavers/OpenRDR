package io.rippledown.chat

import androidx.compose.runtime.*
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE

interface ChatControllerHandler {
    fun sendUserMessage(message: String): Unit
    var onBotMessageReceived: (message: String) -> Unit
}

@Composable
fun ChatController(handler: ChatControllerHandler) {
    val initialBotMessage = BotMessage(CHAT_BOT_INITIAL_MESSAGE)
    var currentUserMessage by remember { mutableStateOf(UserMessage("")) }
    var currentBotMessage by remember { mutableStateOf(initialBotMessage) }
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(listOf(initialBotMessage)) }

    handler.onBotMessageReceived = { message ->
        currentBotMessage = BotMessage(message)
    }

    LaunchedEffect(currentUserMessage) {
        if (currentUserMessage.text.isNotEmpty()) {
            chatHistory = chatHistory + currentUserMessage
        }
    }

    LaunchedEffect(currentBotMessage) {
        if (currentBotMessage.text != initialBotMessage.text) {
            chatHistory = chatHistory + currentBotMessage
        }
    }

    ChatPanel(chatHistory, onMessageSent = { userMessage ->
        currentUserMessage = userMessage
        handler.sendUserMessage(userMessage.text)
    })
}

