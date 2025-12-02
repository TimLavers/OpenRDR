package io.rippledown.mocks

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.rippledown.constants.api.CORNERSTONE
import io.rippledown.constants.api.PORT
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.delay

fun startServer(expectedStatus: CornerstoneStatus): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val server = embeddedServer(Netty, port = PORT) {
        install(WebSockets)
        routing {
            webSocket(CORNERSTONE) {
                // Send expected data
                send(Frame.Text(expectedStatus.toJsonString<CornerstoneStatus>()))

                // Keep open briefly so client receives it, then close
                delay(100)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test Complete"))
            }
        }
    }.start(wait = false)
    println("Server with websocket started")
    return server
}
