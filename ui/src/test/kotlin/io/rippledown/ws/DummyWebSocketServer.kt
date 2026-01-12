package io.rippledown.ws

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.rippledown.constants.api.PORT
import io.rippledown.constants.api.WEB_SOCKET
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.model.KBInfo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.delay

fun startServerAndSendCornerstoneStatus(expectedStatus: CornerstoneStatus) = startServerAndSendPayload(expectedStatus.toJsonString())

fun startServerAndSendRulesSessionCompleted() = startServerAndSendPayload(RULE_SESSION_COMPLETED)

fun startServerAndSendKbInfo(kbInfo: KBInfo) = startServerAndSendPayload(kbInfo.toJsonString())

fun startServerAndSendPayload(payload: String): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val server = embeddedServer(Netty, port = PORT) {
        install(WebSockets)
        routing {
            webSocket(WEB_SOCKET) {
                // Send expected data
                send(Frame.Text(payload))

                // Keep open briefly so client receives it, then close
                delay(100)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test Complete"))
            }
        }
    }.start(wait = false)
    println("Server with websocket started")
    return server
}
