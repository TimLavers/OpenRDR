package io.rippledown.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.rippledown.constants.api.CORNERSTONE
import io.rippledown.server.ServerApplication
import io.rippledown.server.websocket.CornerstoneWebSocketManager

fun Application.webSockets(application: ServerApplication) {
    routing {
        webSocket(path = CORNERSTONE) {
            val webSocketManager = CornerstoneWebSocketManager()

        }

    }
}
