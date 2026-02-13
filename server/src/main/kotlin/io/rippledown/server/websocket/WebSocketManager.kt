package io.rippledown.server.websocket

import io.ktor.websocket.*
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.log.lazyLogger
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString

class WebSocketManager {

    private lateinit var connection: WebSocketSession
    private val logger = lazyLogger

    suspend fun setSession(session: WebSocketSession) {
        connection = session

        try {
            // Keep the session open until the client disconnects
            for (frame in session.incoming) {
                // Handle incoming frames if needed
                frame.readBytes()
            }
        } finally {
            connection.close()
        }
    }

    suspend fun sendStatus(status: CornerstoneStatus) {
        send(status.toJsonString<CornerstoneStatus>())
    }

    suspend fun sendRuleSessionCompleted() {
        send(RULE_SESSION_COMPLETED)
    }

    private suspend fun send(message: String) {
        try {
            if (::connection.isInitialized) {
                connection.send(message)
                logger.info("Sent ws message: $message")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
