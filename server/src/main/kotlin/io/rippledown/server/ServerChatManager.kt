package io.rippledown.server

import io.rippledown.chat.ChatService
import io.rippledown.model.ServerChatResult
import io.rippledown.server.OpenRDRServer.logger
import io.rippledown.server.action.extractAction

class ServerChatManager(
    chatService: ChatService,
    private val actionsInterface: ServerChatActionsInterface
) {
    private var chat = chatService.startChat(emptyList())
    suspend fun sendMessageAndActOnResponse(message: String, kbId: String?): ServerChatResult {
        logger.info("sending message: $message")
        logger.info("chat is: $chat")
        try {
            val contentResponse = chat.sendMessage(message)
            logger.info("got response: $contentResponse")
            val response = contentResponse.text!!
            logger.info("got response text: $response")
            val action = extractAction(response)
            logger.info("got action: $action")
            return action?.doIt(actionsInterface, kbId) ?: ServerChatResult("We didn't understand that.")
        } catch (e: Exception) {
            logger.error("error sending message: $message", e)
            return ServerChatResult("We had an error processing your message.")
        }
    }
}