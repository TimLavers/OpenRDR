package io.rippledown.server.websocket

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.model.KBInfo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test

class WebSocketManagerTest {
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var session: WebSocketSession

    @BeforeTest
    fun setup() {
        webSocketManager = WebSocketManager()
        session = mockk<WebSocketSession>(relaxed = true)
    }

    @Test
    fun setCornerstoneStatusTest() = runBlocking {
        webSocketManager.setSession(session)

        val status = CornerstoneStatus(null, -1, 0)
        webSocketManager.sendCornerstoneStatus(status)
        val expectedText = status.toJsonString<CornerstoneStatus>()
        coVerify {
            session.send(match { it is Frame.Text && it.readText() == expectedText })
        }
    }

    @Test
    fun sendRuleSessionCompletedTest() = runBlocking {
        webSocketManager.setSession(session)

        webSocketManager.sendRuleSessionCompleted()
        coVerify {
            session.send(match { it is Frame.Text && it.readText() == RULE_SESSION_COMPLETED })
        }
    }

    @Test
    fun sendKbInfoTest() = runBlocking {
        webSocketManager.setSession(session)

        val kbInfo = KBInfo("id123", "Blah")
        webSocketManager.sendKbInfo(kbInfo)
        val expectedText = kbInfo.toJsonString<KBInfo>()
        coVerify {
            session.send(match { it is Frame.Text && it.readText() == expectedText })
        }
    }

    @Test
    fun shouldNotSendIfSessionNotSet() = runBlocking {
        val status = CornerstoneStatus(null, -1, 0)
        webSocketManager.sendCornerstoneStatus(status)
        coVerify(exactly = 0) {
            session.send(any())
        }
    }

    @Test
    fun setSessionShouldHandleIncomingFramesUntilClosed() = runBlocking {
        val incoming = Channel<Frame>()
        every { session.incoming } returns incoming

        val job = launch {
            webSocketManager.setSession(session)
        }

        // Send some frames
        incoming.send(Frame.Text("ping"))
        incoming.send(Frame.Text("pong"))

        // Close the channel to end the loop in setSession
        incoming.close()

        // Wait for setSession to complete
        job.join()

        coVerify {
            session.send(match { it is Frame.Close })
        }
    }
}