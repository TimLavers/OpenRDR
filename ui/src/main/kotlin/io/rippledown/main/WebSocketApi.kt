package io.rippledown.main

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.rippledown.constants.api.HOST
import io.rippledown.constants.api.PORT
import io.rippledown.constants.api.WEB_SOCKET
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance

open class WebSocketApi(private val client: HttpClient, private val port: Int = PORT) {
    private val logger = lazyLogger

    open suspend fun startSession(
        updateCornerstoneStatus: (CornerstoneStatus) -> Unit,
        ruleSessionCompleted: () -> Unit
    ) {
        client.webSocket(
            method = HttpMethod.Get,
            host = HOST,
            port = port,
            path = WEB_SOCKET
        ) {
            try {
                incoming.consumeAsFlow()
                    .filterIsInstance<Frame.Text>()
                    .collect { frame ->
                        val receivedText = frame.readText()
                        when {
                            receivedText == RULE_SESSION_COMPLETED -> {
                                ruleSessionCompleted()
                            }

                            else -> handleCornerstoneStatus(receivedText, updateCornerstoneStatus)
                        }
                    }
            } catch (e: Exception) {
                if (e is java.util.concurrent.CancellationException) {
                    //expected during cleanup
                } else {
                    logger.error("WebSocket connection error", e)
                }
            }
        }
    }

    private fun handleCornerstoneStatus(
        message: String,
        updateCornerstoneStatus: (CornerstoneStatus) -> Unit
    ) {
        try {
            val cornerstoneStatus = message.fromJsonString<CornerstoneStatus>()
            updateCornerstoneStatus(cornerstoneStatus)
        } catch (e: Exception) {
            logger.error("Error parsing cornerstone status", e)
        }
    }
}
