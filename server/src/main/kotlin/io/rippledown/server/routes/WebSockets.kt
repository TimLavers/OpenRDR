package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.rippledown.constants.api.CORNERSTONE
import io.rippledown.server.websocket.WebSocketManager

fun Application.webSockets(webSocketManager: WebSocketManager) {
    routing {
        webSocket(path = CORNERSTONE) {
            try {
                webSocketManager.setSession(this)
            } catch (e: Exception) {
                // Handle any connection errors
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error in WebSocket connection: ${e.message}"))
            } finally {
                // Connection will be closed automatically when the block ends
            }

        }

    }
}
