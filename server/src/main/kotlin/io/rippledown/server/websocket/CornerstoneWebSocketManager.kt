package io.rippledown.server.websocket

import io.ktor.websocket.*
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.serialization.json.Json

class CornerstoneWebSocketManager() {

    private lateinit var connection: WebSocketSession

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
        val message = Json.encodeToString(status)
        try {
            connection.send(message)
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }
}
