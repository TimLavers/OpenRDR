package io.rippledown.kb

import io.rippledown.server.websocket.WebSocketManager

class KBSession(val kb: KB, webSocketManager: WebSocketManager? = null) {
    val ruleSessionManager = RuleSessionManager(kb, webSocketManager)
}
