package io.rippledown.main

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.rippledown.constants.api.HOST
import io.rippledown.constants.api.PORT
import io.rippledown.constants.api.WEB_SOCKET
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.PresenceInfo
import io.rippledown.model.change.KbChange
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance

open class WebSocketApi(
    private val client: HttpClient,
    private val port: Int = PORT,
    private val retryDelay: suspend (Long) -> Unit = { delay(it) }
) {
    private val logger = lazyLogger

    open suspend fun startSession(
        updateCornerstoneStatus: (CornerstoneStatus) -> Unit,
        ruleSessionCompleted: () -> Unit,
        updateCasesInfo: (CasesInfo) -> Unit = {},
        kbInfoUpdated: (KBInfo) -> Unit = {},
        kbClosed: () -> Unit = {},
        chatContextChanged: (ChatContextInfo) -> Unit = {},
        presenceChanged: (PresenceInfo) -> Unit = {},
        kbChanged: (KbChange) -> Unit = {},
        connected: (Boolean) -> Unit = {}
    ) {
        var backoffMillis = 1_000L
        connected(false)
        while (currentCoroutineContext().isActive) {
            var opened = false
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = HOST,
                    port = port,
                    path = WEB_SOCKET
                ) {
                    opened = true
                    backoffMillis = 1_000L
                    // Subscribe before the caller starts its REST recovery snapshots.
                    connected(true)
                    incoming.consumeAsFlow()
                        .filterIsInstance<Frame.Text>()
                        .collect { frame ->
                            val receivedText = frame.readText()
                            when {
                                receivedText == RULE_SESSION_COMPLETED -> {
                                    ruleSessionCompleted()
                                }

                                receivedText.startsWith(CASES_INFO_PREFIX) -> {
                                    handleCasesInfo(receivedText, updateCasesInfo)
                                }

                                receivedText == KB_CLOSED -> kbClosed()

                                receivedText.startsWith(KB_INFO_PREFIX) -> handleKbInfo(receivedText, kbInfoUpdated)

                                receivedText.startsWith(CHAT_CONTEXT_PREFIX) ->
                                    handleChatContext(receivedText, chatContextChanged)

                                receivedText.startsWith(PRESENCE_PREFIX) ->
                                    handlePresence(receivedText, presenceChanged)

                                receivedText.startsWith(KB_CHANGED_PREFIX) ->
                                    handleKbChange(receivedText, kbChanged)

                                else -> handleCornerstoneStatus(receivedText, updateCornerstoneStatus)
                            }
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("WebSocket disconnected; retrying in $backoffMillis ms", e)
            } finally {
                if (opened) connected(false)
            }
            retryDelay(backoffMillis)
            backoffMillis = (backoffMillis * 2).coerceAtMost(30_000L)
        }
    }

    private fun handleCasesInfo(
        message: String,
        updateCasesInfo: (CasesInfo) -> Unit
    ) {
        try {
            val json = message.removePrefix(CASES_INFO_PREFIX)
            val casesInfo = json.fromJsonString<CasesInfo>()
            updateCasesInfo(casesInfo)
        } catch (e: Exception) {
            logger.error("Error parsing cases info", e)
        }
    }

    private fun handleKbInfo(message: String, kbInfoUpdated: (KBInfo) -> Unit) {
        try {
            kbInfoUpdated(message.removePrefix(KB_INFO_PREFIX).fromJsonString<KBInfo>())
        } catch (e: Exception) {
            logger.error("Error parsing kb info", e)
        }
    }

    private fun handleChatContext(message: String, chatContextChanged: (ChatContextInfo) -> Unit) {
        try {
            chatContextChanged(message.removePrefix(CHAT_CONTEXT_PREFIX).fromJsonString<ChatContextInfo>())
        } catch (e: Exception) {
            logger.error("Error parsing chat context", e)
        }
    }

    private fun handlePresence(message: String, presenceChanged: (PresenceInfo) -> Unit) {
        try {
            presenceChanged(message.removePrefix(PRESENCE_PREFIX).fromJsonString<PresenceInfo>())
        } catch (e: Exception) {
            logger.error("Error parsing presence info", e)
        }
    }

    private fun handleKbChange(message: String, kbChanged: (KbChange) -> Unit) {
        try {
            kbChanged(message.removePrefix(KB_CHANGED_PREFIX).fromJsonString<KbChange>())
        } catch (e: Exception) {
            logger.error("Error parsing KB change", e)
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
