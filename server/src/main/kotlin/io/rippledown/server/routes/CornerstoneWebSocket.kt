package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.rippledown.server.websocket.CornerstoneWebSocketManager
import kotlin.time.Duration.Companion.seconds

fun Application.configureCornerstoneWebSocket(webSocketManager: CornerstoneWebSocketManager) {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/cornerstone-status") {
            try {
                webSocketManager.setSession(this)
                // Keep the connection open by consuming incoming frames
                for (frame in incoming) {
                    // Just consume the frames to keep the connection alive
                    // We don't need to process them for this use case
                }
            } catch (e: Exception) {
                // Handle any connection errors
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error in WebSocket connection: ${e.message}"))
            } finally {
                // Connection will be closed automatically when the block ends
            }
        }
    }
}
