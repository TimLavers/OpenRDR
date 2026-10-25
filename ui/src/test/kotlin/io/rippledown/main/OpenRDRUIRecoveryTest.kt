package io.rippledown.main

import androidx.compose.ui.test.*
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.chat.ChatTestHook
import io.rippledown.chat.NoticeMessage
import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.model.*
import io.rippledown.model.change.*
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCaseWithInterpretation
import io.rippledown.voice.VoiceRecognition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class OpenRDRUIRecoveryTest {
    private val kb = KBInfo("clinic", "Clinic")
    private val alice = UserId("Alice")
    private val api = mockk<Api>()
    private val handler = mockk<Handler>()
    private val voice = mockk<VoiceRecognition>()
    private var connected: (Boolean) -> Unit = {}
    private var kbChanged: (KbChange) -> Unit = {}
    private var contextChanged: (ChatContextInfo) -> Unit = {}
    private var presenceChanged: (PresenceInfo) -> Unit = {}
    private var sessionChanged: (CornerstoneStatus) -> Unit = {}
    private var sessionCompleted: () -> Unit = {}
    private var connectImmediately = false

    @BeforeEach
    fun setUp() {
        ChatTestHook.reset()
        every { voice.isListening } returns MutableStateFlow(false)
        every { voice.isTranscribing } returns MutableStateFlow(false)
        every { voice.partialResult } returns MutableStateFlow("")
        every { api.userId } returns alice
        every { handler.api } returns api
        every { handler.isClosing } returns { true }
        coEvery { api.chatContext() } returns ChatContextInfo(kb, 1L)
        coEvery { api.presence() } returns emptyList()
        coEvery { api.changes(any()) } returns emptyList()
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(CaseId(1, "Case")))
        coEvery { api.getCase(1) } returns caseWith("Original comment.")
        coEvery { api.kbDescription(any()) } returns ""
        coEvery { api.startConversation(any(), any()) } returns ChatResponse("")
        coEvery { api.startWebSocketSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            sessionChanged = arg(0)
            sessionCompleted = arg(1)
            contextChanged = arg(5)
            presenceChanged = arg(6)
            kbChanged = arg(7)
            connected = arg(8)
            if (connectImmediately) connected(true)
        }
    }

    private fun caseWith(text: String) = createViewableCaseWithInterpretation("Case", 1, listOf(text))
    private fun record(id: Int, user: String = "Bob") =
        ChangeRecord(id, user, id.toLong(), ChangeKind.RULES_ADDED, "Change $id.")
    private fun notices() = ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().map { it.text }

    @Test
    fun `a connection that drops before recovery starts does not lose its cursor`() = runComposeUiTest {
        // Given
        coEvery { api.changes(null) } returns listOf(record(1))
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        runOnIdle { connected(false) }
        waitForIdle()
        coEvery { api.changes(1) } returns listOf(record(2), record(3))

        // When a brief connection receives a frame and drops before recomposition
        runOnIdle {
            connected(true)
            kbChanged(KbChange(kb.id, record(3)))
            connected(false)
        }
        waitForIdle()
        runOnIdle { connected(true) }

        // Then
        waitForIdle()
        coVerify(exactly = 1) { api.changes(1) }
        notices() shouldBe listOf("Bob: Change 1.", "Bob: Change 3.", "Bob: Change 2.")
    }

    @Test
    fun `an immediately connected socket starts only one replay`() = runComposeUiTest {
        // Given
        connectImmediately = true
        coEvery { api.changes(null) } returns listOf(record(1))

        // When
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }

        // Then
        onNodeWithText("Original comment.").assertExists()
        coVerify(exactly = 1) { api.changes(null) }
        notices() shouldBe listOf("Bob: Change 1.")
    }

    @Test
    fun `connection status is visible and recovery starts only after subscribing`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        onNodeWithContentDescription("Reconnecting").assertTextEquals("Reconnecting…").assertHasNoClickAction()
        coVerify(exactly = 0) { api.changes(any()) }
        coEvery { api.getCase(1) } returns caseWith("Recovered comment.")
        coEvery { api.presence() } returns listOf(PresentUser("Bob", false))
        coEvery { api.changes(null) } returns listOf(record(1), record(2, "Alice"))

        // When
        runOnIdle { connected(true) }

        // Then
        onNodeWithText("Recovered comment.").assertExists()
        onNodeWithContentDescription("Reconnecting").assertDoesNotExist()
        onNodeWithContentDescription(PRESENCE_DESCRIPTION).assertTextEquals("Also here: Bob")
        notices() shouldBe listOf("Bob: Change 1.")
        coVerify(exactly = 1) { api.changes(null) }
        coVerify(exactly = 2) { api.presence() }

        // When
        runOnIdle { connected(false) }

        // Then
        onNodeWithContentDescription("Reconnecting").assertTextEquals("Reconnecting…")
    }

    @Test
    fun `recovery captures the cursor before live frames and deduplicates their replay`() = runComposeUiTest {
        // Given
        coEvery { api.changes(null) } returns listOf(record(1))
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        runOnIdle { connected(false) }
        waitForIdle()
        val replay = CompletableDeferred<List<ChangeRecord>>()
        coEvery { api.changes(1) } coAnswers { replay.await() }

        // When a live frame arrives before the recovery effect has started
        runOnIdle {
            connected(true)
            kbChanged(KbChange(kb.id, record(3)))
        }
        waitForIdle()
        coVerify(exactly = 1) { api.changes(1) }
        runOnIdle { replay.complete(listOf(record(2), record(3))) }

        // Then
        waitForIdle()
        notices() shouldBe listOf("Bob: Change 1.", "Bob: Change 3.", "Bob: Change 2.")

        // When the same record is returned on the next reconnect
        coEvery { api.changes(3) } returns listOf(record(3))
        runOnIdle { connected(false) }
        waitForIdle()
        runOnIdle { connected(true) }

        // Then
        waitForIdle()
        coVerify(exactly = 1) { api.changes(3) }
        notices().size shouldBe 3
    }

    @Test
    fun `a failed replay preserves its cursor despite newer live changes`() = runComposeUiTest {
        // Given
        coEvery { api.changes(null) } returns listOf(record(1))
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        runOnIdle { connected(false) }
        waitForIdle()
        coEvery { api.changes(1) } throws IllegalStateException("Unavailable")

        // When recovery fails while later live changes are arriving
        runOnIdle {
            connected(true)
            kbChanged(KbChange(kb.id, record(3)))
        }
        waitForIdle()
        runOnIdle { connected(false) }
        waitForIdle()
        coEvery { api.changes(1) } returns listOf(record(2), record(3))
        runOnIdle { connected(true) }

        // Then the missing record is still fetched
        waitForIdle()
        coVerify(exactly = 2) { api.changes(1) }
        notices() shouldBe listOf("Bob: Change 1.", "Bob: Change 3.", "Bob: Change 2.")
    }

    @Test
    fun `switching KB during replay discards the old response and recovers the new KB`() = runComposeUiTest {
        // Given
        val replay = CompletableDeferred<List<ChangeRecord>>()
        coEvery { api.changes(null) } coAnswers { replay.await() }
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()

        // When
        coEvery { api.changes(null) } returns listOf(record(1, "Carol"))
        coEvery { api.getCase(1) } returns caseWith("Other KB comment.")
        runOnIdle { contextChanged(ChatContextInfo(KBInfo("other", "Other"), 1L)) }
        onNodeWithText("Other KB comment.").assertExists()
        runOnIdle { replay.complete(listOf(record(1))) }

        // Then
        waitForIdle()
        notices() shouldBe listOf("Carol: Change 1.")
        coVerify(exactly = 2) { api.changes(null) }
    }

    @Test
    fun `reconnect during rule building defers the case refresh until completion`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        runOnIdle { sessionChanged(CornerstoneStatus()) }
        coEvery { api.getCase(1) } returns caseWith("Recovered comment.")
        coEvery { api.changes(null) } returns listOf(record(1))

        // When
        runOnIdle { connected(true) }

        // Then
        waitForIdle()
        coVerify(exactly = 1) { api.getCase(1) }
        notices() shouldBe listOf("Bob: Change 1.")

        // When
        runOnIdle { sessionCompleted() }

        // Then
        onNodeWithText("Recovered comment.").assertExists()
        coVerify(exactly = 2) { api.getCase(1) }
    }

    @Test
    fun `a live presence push wins over the reconnect snapshot`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        val presence = CompletableDeferred<List<PresentUser>>()
        coEvery { api.presence() } coAnswers { presence.await() }
        runOnIdle { connected(true) }
        waitForIdle()

        // When
        runOnIdle { presenceChanged(PresenceInfo(kb.id, listOf(PresentUser("Carol", false)))) }
        runOnIdle { presence.complete(listOf(PresentUser("Bob", false))) }

        // Then
        onNodeWithContentDescription(PRESENCE_DESCRIPTION).assertTextEquals("Also here: Carol")
    }

    @Test
    fun `a KB adopted after the socket connects still recovers its changes`() = runComposeUiTest {
        // Given
        val context = CompletableDeferred<ChatContextInfo?>()
        coEvery { api.chatContext() } coAnswers { context.await() }
        coEvery { api.changes(null) } returns listOf(record(1))
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        coVerify(exactly = 0) { api.changes(any()) }

        // When
        runOnIdle { context.complete(ChatContextInfo(kb, 1L)) }

        // Then
        onNodeWithText("Original comment.").assertExists()
        notices() shouldBe listOf("Bob: Change 1.")
        coVerify(exactly = 1) { api.changes(null) }
    }

    @Test
    fun `failed case and presence snapshots do not prevent change replay`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } throws IllegalStateException("Unavailable")
        coEvery { api.presence() } throws IllegalStateException("Unavailable")
        coEvery { api.changes(null) } returns listOf(record(1))

        // When
        runOnIdle { connected(true) }

        // Then
        waitForIdle()
        onNodeWithText("Original comment.").assertExists()
        notices() shouldBe listOf("Bob: Change 1.")
        onNodeWithContentDescription("Reconnecting").assertDoesNotExist()
    }
}
