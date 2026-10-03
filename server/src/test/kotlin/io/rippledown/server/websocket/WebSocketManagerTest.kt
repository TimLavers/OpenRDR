package io.rippledown.server.websocket

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.websocket.*
import io.mockk.*
import io.rippledown.constants.chat.CASES_INFO_PREFIX
import io.rippledown.constants.chat.KB_CLOSED
import io.rippledown.constants.chat.KB_INFO_PREFIX
import io.rippledown.fromJsonString
import io.rippledown.model.*
import io.rippledown.toJsonString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.test.Test

class WebSocketManagerTest {
    private val carol = UserId("carol")

    @Test
    fun `CASES_INFO_PREFIX should be the expected value`() {
        //Then
        CASES_INFO_PREFIX shouldBe "CasesInfo:"
    }

    @Test
    fun `should format CasesInfo message with prefix followed by JSON`() {
        //Given
        val casesInfo = CasesInfo(
            caseIds = listOf(CaseId(id = 1, name = "Case1"), CaseId(id = 2, name = "Case2")),
            cornerstoneCaseIds = listOf(CaseId(id = 3, name = "CS1", type = CaseListType.Cornerstone)),
            userDefinedCaseLists = listOf(
                CaseListInfo("Good", listOf(CaseId(id = 4, name = "U1", type = CaseListType("Good"))))
            ),
            kbName = "TestKB"
        )

        //When
        val message = CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>()

        //Then
        message shouldStartWith CASES_INFO_PREFIX
        val json = message.removePrefix(CASES_INFO_PREFIX)
        val parsed = json.fromJsonString<CasesInfo>()
        parsed shouldBe casesInfo
        parsed.caseIds.size shouldBe 2
        parsed.cornerstoneCaseIds.size shouldBe 1
        parsed.userDefinedCaseLists.size shouldBe 1
        parsed.userDefinedCaseLists[0].name shouldBe "Good"
        parsed.kbName shouldBe "TestKB"
    }

    @Test
    fun `KB_INFO_PREFIX and KB_CLOSED should be the expected values`() {
        //Then
        KB_INFO_PREFIX shouldBe "KbInfo:"
        KB_CLOSED shouldBe "KbClosed"
    }

    @Test
    fun `sendKbInfo sends the prefix followed by the KBInfo as JSON`() = withConnectedManager { manager, sent ->
        //Given
        val kbInfo = KBInfo("glucose_123", "Glucose")

        //When
        manager.sendKbInfo(carol, kbInfo)

        //Then
        sent.size shouldBe 1
        sent[0] shouldStartWith KB_INFO_PREFIX
        sent[0].removePrefix(KB_INFO_PREFIX).fromJsonString<KBInfo>() shouldBe kbInfo
    }

    @Test
    fun `sendKbClosed sends the KB_CLOSED marker`() = withConnectedManager { manager, sent ->
        //When
        manager.sendKbClosed(carol)

        //Then
        sent shouldBe listOf(KB_CLOSED)
    }

    @Test
    fun `sending before a session is connected is a no-op`() = runBlocking {
        //Given
        val manager = WebSocketManager()

        //When / Then - no exception
        manager.sendKbInfo(carol, KBInfo("glucose_123", "Glucose"))
        manager.sendKbClosed(carol)
    }

    @Test
    fun `a client disconnecting after another has connected does not close the new connection`() = runBlocking {
        //Given
        val manager = WebSocketManager()
        val oldIncoming = Channel<Frame>()
        val oldFrames = mutableListOf<Frame>()
        val oldSession = sessionMock(oldIncoming, oldFrames)
        val oldJob = launch { manager.setSession(carol, oldSession) }
        yield()
        val newIncoming = Channel<Frame>()
        val newFrames = mutableListOf<Frame>()
        val newSession = sessionMock(newIncoming, newFrames)
        val newJob = launch { manager.setSession(carol, newSession) }
        yield()

        //When
        oldIncoming.close()
        oldJob.join()
        manager.sendKbClosed(carol)

        //Then
        oldFrames.filterIsInstance<Frame.Close>().size shouldBe 1
        newFrames.filterIsInstance<Frame.Close>().size shouldBe 0
        newFrames.filterIsInstance<Frame.Text>().map { it.readText() } shouldBe listOf(KB_CLOSED)
        newIncoming.close()
        newJob.join()
    }

    private fun sessionMock(incoming: Channel<Frame>, frames: MutableList<Frame>) =
        sessionMock(incoming) { frames.add(it) }

    private fun sessionMock(incoming: Channel<Frame>, onFrame: (Frame) -> Unit): WebSocketSession {
        val session = mockk<WebSocketSession>()
        every { session.incoming } returns incoming
        coEvery { session.flush() } just Runs
        coEvery { session.send(any<Frame>()) } answers { onFrame(firstArg()) }
        return session
    }

    private fun withConnectedManager(block: suspend (WebSocketManager, List<String>) -> Unit) = runBlocking {
        val sent = mutableListOf<String>()
        val incoming = Channel<Frame>()
        val session = sessionMock(incoming) { if (it is Frame.Text) sent.add(it.readText()) }
        val manager = WebSocketManager()
        val sessionJob = launch { manager.setSession(carol, session) }
        yield()
        block(manager, sent)
        incoming.close()
        sessionJob.join()
    }

    private class ConnectedUser(val userId: UserId, val incoming: Channel<Frame>, val sent: MutableList<String>) {
        lateinit var job: Job
    }

    private fun CoroutineScope.connect(manager: WebSocketManager, name: String): ConnectedUser {
        val user = ConnectedUser(UserId(name), Channel(), mutableListOf())
        val session = sessionMock(user.incoming) { if (it is Frame.Text) user.sent.add(it.readText()) }
        user.job = launch { manager.setSession(user.userId, session) }
        return user
    }

    private suspend fun ConnectedUser.disconnect() {
        incoming.close()
        job.join()
    }

    @Test
    fun `sendToUser reaches only the addressee`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val alice = connect(manager, "alice")
        val bob = connect(manager, "bob")
        yield()

        // When
        manager.sendToUser(alice.userId, "for alice")

        // Then
        alice.sent shouldBe listOf("for alice")
        bob.sent shouldBe emptyList()
        alice.disconnect()
        bob.disconnect()
    }

    @Test
    fun `sendToUser reaches every window of that user`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val window1 = connect(manager, "alice")
        val window2 = connect(manager, "alice")
        yield()

        // When
        manager.sendToUser(UserId("alice"), "hello")

        // Then
        window1.sent shouldBe listOf("hello")
        window2.sent shouldBe listOf("hello")
        window1.disconnect()
        window2.disconnect()
    }

    @Test
    fun `sendToUser to a user with no connection is a no-op`() = runBlocking {
        // Given
        val manager = WebSocketManager()

        // When / Then - no exception
        manager.sendToUser(UserId("nobody"), "hello")
    }

    @Test
    fun `broadcast reaches every connected user`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val alice = connect(manager, "alice")
        val bob = connect(manager, "bob")
        yield()

        // When
        manager.broadcast("everyone")

        // Then
        alice.sent shouldBe listOf("everyone")
        bob.sent shouldBe listOf("everyone")
        alice.disconnect()
        bob.disconnect()
    }

    @Test
    fun `a user whose last connection closed is deregistered`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val alice = connect(manager, "alice")
        val bob = connect(manager, "bob")
        yield()
        manager.connectedUsers() shouldBe setOf(alice.userId, bob.userId)

        // When
        alice.disconnect()
        manager.sendToUser(alice.userId, "too late")
        manager.broadcast("still here")

        // Then
        manager.connectedUsers() shouldBe setOf(bob.userId)
        alice.sent shouldBe emptyList()
        bob.sent shouldBe listOf("still here")
        bob.disconnect()
    }

    @Test
    fun `a user with one window left stays registered when another closes`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val window1 = connect(manager, "alice")
        val window2 = connect(manager, "alice")
        yield()

        // When
        window1.disconnect()
        manager.sendToUser(UserId("alice"), "hello")

        // Then
        manager.connectedUsers() shouldBe setOf(UserId("alice"))
        window1.sent shouldBe emptyList()
        window2.sent shouldBe listOf("hello")
        window2.disconnect()
    }

    @Test
    fun `a send that throws does not propagate or stop other deliveries`() = runBlocking {
        // Given
        val manager = WebSocketManager()
        val deadIncoming = Channel<Frame>()
        val dead = mockk<WebSocketSession>()
        every { dead.incoming } returns deadIncoming
        coEvery { dead.flush() } just Runs
        coEvery { dead.send(any<Frame>()) } answers {
            if (firstArg<Frame>() is Frame.Text) throw IllegalStateException("gone")
        }
        val deadJob = launch { manager.setSession(UserId("alice"), dead) }
        val bob = connect(manager, "bob")
        yield()

        // When
        manager.broadcast("hello")

        // Then
        bob.sent shouldBe listOf("hello")
        deadIncoming.close()
        deadJob.join()
        bob.disconnect()
    }

    @Test
    fun `should roundtrip empty CasesInfo through prefix format`() {
        //Given
        val casesInfo = CasesInfo()

        //When
        val message = CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>()
        val json = message.removePrefix(CASES_INFO_PREFIX)
        val parsed = json.fromJsonString<CasesInfo>()

        //Then
        parsed shouldBe casesInfo
        parsed.count shouldBe 0
    }
}
