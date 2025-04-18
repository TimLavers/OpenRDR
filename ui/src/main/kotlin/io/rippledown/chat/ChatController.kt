package io.rippledown.chat

import androidx.compose.runtime.*
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE

interface ChatControllerHandler {
    fun sendMessage(message: String): String
    fun onMessageReceived(message: String)
}

@Composable
fun ChatController(handler: ChatControllerHandler) {
    val initialBotMessage = BotMessage(CHAT_BOT_INITIAL_MESSAGE)
    var chatHistory: List<ChatMessage> by remember { mutableStateOf(listOf(initialBotMessage)) }
    var currentUserText by remember { mutableStateOf("") }

    LaunchedEffect(currentUserText) {
        if (currentUserText.isNotEmpty()) {
            val message = UserMessage(currentUserText)
            chatHistory = chatHistory + message
            currentUserText = ""
        }
    }


    ChatPanel(chatHistory, onMessageSent = { message -> currentUserText = message.text })
}

