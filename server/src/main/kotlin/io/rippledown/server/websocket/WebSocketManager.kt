package io.rippledown.server.websocket

import io.ktor.websocket.*
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
                val bytes = frame.readBytes()
                println("Received frame: ${bytes.decodeToString()}")
            }
        } finally {
            connection.close()
        }
    }

    suspend fun sendStatus(status: CornerstoneStatus) {
        val message = status.toJsonString()
        logger.info("Sending cornerstone status: $message")
        try {
            if (::connection.isInitialized) {
                connection.send(message)
                logger.info("Sent cornerstone status: $message")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
