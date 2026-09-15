package io.rippledown.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest

class ChatState {
    var history: List<ChatMessage> by mutableStateOf(emptyList())
        private set
    var awaitingResponse by mutableStateOf(false)
        private set

    fun userMessage(message: UserMessage) {
        if (message.text.isNotEmpty()) history = history + message
        awaitingResponse = true
    }

    fun localMessage(text: String) {
        history = history + BotMessage(text)
    }

    fun receive(response: ChatResponse, onFileDialogRequested: (KbFileDialogRequest) -> Unit) {
        val lastBotMessage = history.lastOrNull()
        val isDuplicate = when (lastBotMessage) {
            is KbChoiceListMessage -> lastBotMessage.text == response.text && lastBotMessage.listing == response.kbListing
            is BotMessage -> lastBotMessage.text == response.text && response.kbListing == null
            else -> false
        }
        if (!isDuplicate) {
            history = history + buildList {
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
        }
        response.kbFileDialogRequest?.let(onFileDialogRequested)
        awaitingResponse = false
    }
}
