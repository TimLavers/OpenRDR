package io.rippledown.server.websocket

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.websocket.*
import io.mockk.*
import io.rippledown.constants.chat.CASES_INFO_PREFIX
import io.rippledown.constants.chat.KB_CLOSED
import io.rippledown.constants.chat.KB_INFO_PREFIX
import io.rippledown.fromJsonString
import io.rippledown.model.CaseId
import io.rippledown.model.CaseType
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.toJsonString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test

class WebSocketManagerTest {

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
            cornerstoneCaseIds = listOf(CaseId(id = 3, name = "CS1", type = CaseType.Cornerstone)),
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
        manager.sendKbInfo(kbInfo)

        //Then
        sent.size shouldBe 1
        sent[0] shouldStartWith KB_INFO_PREFIX
        sent[0].removePrefix(KB_INFO_PREFIX).fromJsonString<KBInfo>() shouldBe kbInfo
    }

    @Test
    fun `sendKbClosed sends the KB_CLOSED marker`() = withConnectedManager { manager, sent ->
        //When
        manager.sendKbClosed()

        //Then
        sent shouldBe listOf(KB_CLOSED)
    }

    @Test
    fun `sending before a session is connected is a no-op`() = runBlocking {
        //Given
        val manager = WebSocketManager()

        //When / Then - no exception
        manager.sendKbInfo(KBInfo("glucose_123", "Glucose"))
        manager.sendKbClosed()
    }

    private fun withConnectedManager(block: suspend (WebSocketManager, List<String>) -> Unit) = runBlocking {
        val sent = mutableListOf<String>()
        val incoming = Channel<Frame>()
        val session = mockk<WebSocketSession>()
        every { session.incoming } returns incoming
        coEvery { session.flush() } just Runs
        coEvery { session.send(any<Frame>()) } answers {
            val frame = firstArg<Frame>()
            if (frame is Frame.Text) sent.add(frame.readText())
        }
        val manager = WebSocketManager()
        val sessionJob = launch { manager.setSession(session) }
        yield()
        block(manager, sent)
        incoming.close()
        sessionJob.join()
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
