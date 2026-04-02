package io.rippledown.kb

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.server.websocket.WebSocketManager

class KBSession(val kb: KB, webSocketManager: WebSocketManager? = null) {
    val ruleSessionManager = RuleSessionManager(kb, webSocketManager)
    val chatSessionManager = ChatSessionManager(ruleSessionManager)

    suspend fun startConversation(viewableCase: ViewableCase): ChatResponse =
        chatSessionManager.startConversation(viewableCase)

    suspend fun responseToUserMessage(message: String): ChatResponse =
        chatSessionManager.responseToUserMessage(message)
}
