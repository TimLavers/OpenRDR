package io.rippledown.main

import androidx.compose.ui.test.*
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.chat.CHAT_SEND
import io.rippledown.chat.CHAT_TEXT_FIELD
import io.rippledown.chat.ChatTestHook
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.cornerstone.CornerstoneTestHook
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalTestApi::class)
class OpenRDRUIReconnectStateTest {
    private val kb = KBInfo("clinic", "Clinic")
    private val other = KBInfo("other", "Other clinic")
    private val alice = UserId("Alice")
    private val api = mockk<Api>()
    private val handler = mockk<Handler>()
    private val voice = mockk<VoiceRecognition>()
    private var connected: (Boolean) -> Unit = {}
    private var contextChanged: (ChatContextInfo) -> Unit = {}
    private var sessionChanged: (CornerstoneStatus) -> Unit = {}
    private var sessionCompleted: () -> Unit = {}
    private var casesChanged: (CasesInfo) -> Unit = {}

    @BeforeEach
    fun setUp() {
        ChatTestHook.reset()
        CornerstoneTestHook.reset()
        every { voice.isListening } returns MutableStateFlow(false)
        every { voice.isTranscribing } returns MutableStateFlow(false)
        every { voice.partialResult } returns MutableStateFlow("")
        every { api.userId } returns alice
        every { handler.api } returns api
        every { handler.isClosing } returns { true }
        coEvery { api.chatContext() } returns ChatContextInfo(kb, 1)
        coEvery { api.presence() } returns emptyList()
        coEvery { api.changes(any()) } returns emptyList()
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(CaseId(1, "First case"), CaseId(2, "Second case")))
        coEvery { api.getCase(1) } returns caseWith("Original comment.")
        coEvery { api.getCase(2) } returns createViewableCaseWithInterpretation(
            "Second case",
            2,
            listOf("Second comment.")
        )
        coEvery { api.kbDescription(any()) } returns ""
        coEvery { api.startConversation(any(), any()) } returns ChatResponse("")
        coEvery { api.startWebSocketSession(any(), any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            sessionChanged = arg(0)
            sessionCompleted = arg(1)
            casesChanged = arg(2)
            contextChanged = arg(5)
            connected = arg(8)
        }
    }

    private fun caseWith(text: String) = createViewableCaseWithInterpretation("First case", 1, listOf(text))
    private fun cornerstone() = CornerstoneStatus(
        createViewableCaseWithInterpretation("Cornerstone", 99, listOf("Prior advice.")), 0, 1
    )

    private suspend fun ComposeUiTest.startAndDisconnect() {
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        runOnIdle { connected(false) }
        waitForIdle()
    }

    @Test
    fun `a drafted message cannot be sent until reconnect has recovered the context`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        runOnIdle { connected(true) }
        waitForIdle()
        onNodeWithContentDescription(CHAT_TEXT_FIELD).performTextInput("Add a comment")
        runOnIdle { connected(false) }
        val snapshot = CompletableDeferred<ChatContextInfo?>()
        coEvery { api.chatContext() } coAnswers { snapshot.await() }

        // When
        runOnIdle { connected(true) }
        waitForIdle()

        // Then
        onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsNotEnabled()
        onNodeWithContentDescription(CHAT_SEND).assertIsNotEnabled()
        onNodeWithContentDescription(CHAT_SEND).performClick()
        coVerify(exactly = 0) { api.sendUserMessage(any()) }

        // When
        runOnIdle { snapshot.complete(ChatContextInfo(kb, 1)) }

        // Then
        onNodeWithContentDescription(CHAT_TEXT_FIELD).assertIsEnabled()
        onNodeWithContentDescription(CHAT_SEND).assertIsEnabled()
    }

    @Test
    fun `reconnect adopts the other window's KB before fetching its state without restarting chat`() =
        runComposeUiTest {
            // Given
            startAndDisconnect()
            var contextRead = false
            coEvery { api.chatContext() } coAnswers { contextRead = true; ChatContextInfo(other, 2) }
            coEvery { api.waitingCasesInfo() } coAnswers {
                contextRead shouldBe true
                CasesInfo(listOf(CaseId(2, "Second case")), kbName = other.name)
            }
            coEvery { api.presence() } coAnswers { contextRead shouldBe true; emptyList() }
            coEvery { api.changes(any()) } coAnswers { contextRead shouldBe true; emptyList() }

            // When
            runOnIdle { connected(true) }

            // Then
            onNodeWithText(other.name).assertExists()
            onNodeWithText("Second comment.").assertExists()
            coVerify(exactly = 0) { api.startConversation(other.id, any()) }
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `reconnect clears a closed KB or a vanished server conversation`(conversationExists: Boolean) =
        runComposeUiTest {
            // Given
            startAndDisconnect()
            coEvery { api.chatContext() } returns if (conversationExists) ChatContextInfo(null, null) else null
            clearMocks(api, answers = false, recordedCalls = true)

            // When
            runOnIdle { connected(true) }

            // Then
            onNodeWithText(NO_KB_SELECTED).assertExists()
            onNodeWithText("Original comment.").assertDoesNotExist()
            coVerify(exactly = 0) { api.changes(any()) }
            coVerify(exactly = 0) { api.waitingCasesInfo() }
            coVerify(exactly = if (conversationExists) 0 else 1) { api.startConversation(null, null) }
        }

    @Test
    fun `a newer context push wins over a late reconnect context response`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        val snapshot = CompletableDeferred<ChatContextInfo?>()
        coEvery { api.chatContext() } coAnswers { snapshot.await() }
        runOnIdle { connected(true) }
        waitForIdle()

        // When
        runOnIdle { contextChanged(ChatContextInfo(other, 2)) }
        runOnIdle { snapshot.complete(ChatContextInfo(kb, 1)) }

        // Then
        onNodeWithText(other.name).assertExists()
        onNodeWithText("Second comment.").assertExists()
        coVerify(exactly = 0) { api.startConversation(other.id, any()) }
    }

    @Test
    fun `reconnect clears a rule completed while offline and releases deferred refreshes`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        runOnIdle { sessionChanged(cornerstone()) }
        waitForIdle()
        CornerstoneTestHook.snapshot(alice).isShowing shouldBe true
        coEvery { api.getCase(1) } returns caseWith("Committed comment.")
        coEvery { api.changes(any()) } returns listOf(
            ChangeRecord(
                1,
                "Alice",
                1,
                ChangeKind.RULES_ADDED,
                "Added comment."
            )
        )

        // When
        runOnIdle { connected(true) }

        // Then
        onNodeWithText("Committed comment.").assertExists()
        CornerstoneTestHook.snapshot(alice).isShowing shouldBe false
    }

    @Test
    fun `a still active session is recovered even if its start frame was missed`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        coEvery { api.cornerstoneStatus() } returns cornerstone()

        // When
        runOnIdle { connected(true) }
        waitForIdle()

        // Then
        CornerstoneTestHook.snapshot(alice).cornerstoneCaseName shouldBe "Cornerstone"
    }

    @Test
    fun `a completion push wins over an old active session snapshot`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        runOnIdle { sessionChanged(cornerstone()) }
        val snapshot = CompletableDeferred<CornerstoneStatus?>()
        coEvery { api.cornerstoneStatus() } coAnswers { snapshot.await() }
        coEvery { api.getCase(1) } returns caseWith("Committed comment.")
        runOnIdle { connected(true) }
        waitForIdle()

        // When
        runOnIdle { sessionCompleted() }
        runOnIdle { snapshot.complete(cornerstone()) }

        // Then
        onNodeWithText("Committed comment.").assertExists()
        CornerstoneTestHook.snapshot(alice).isShowing shouldBe false
    }

    @Test
    fun `reconnect reloads lists and selects a surviving case when the selected case was deleted`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(CaseId(2, "Second case")))
        coEvery { api.getCase(1) } returns null
        coEvery { api.changes(any()) } returns listOf(
            ChangeRecord(
                1,
                "Bob",
                1,
                ChangeKind.CASE_DELETED,
                "Deleted First case."
            )
        )

        // When
        runOnIdle { connected(true) }

        // Then
        onNodeWithText("Second comment.").assertExists()
        onNodeWithText("Original comment.").assertDoesNotExist()
        onNodeWithText("First case").assertDoesNotExist()
    }

    @Test
    fun `reconnect clears the displayed case when the last case was deleted`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        coEvery { api.waitingCasesInfo() } returns CasesInfo()
        coEvery { api.getCase(1) } returns null

        // When
        runOnIdle { connected(true) }

        // Then
        onNodeWithText("Original comment.").assertDoesNotExist()
        coVerify { api.startConversation(kb.id, null) }
    }

    @Test
    fun `a live case list wins over an older reconnect snapshot`() = runComposeUiTest {
        // Given
        startAndDisconnect()
        val snapshot = CompletableDeferred<CasesInfo>()
        coEvery { api.waitingCasesInfo() } coAnswers { snapshot.await() }
        runOnIdle { connected(true) }
        waitForIdle()

        // When
        runOnIdle { casesChanged(CasesInfo(listOf(CaseId(2, "Second case")), kbName = kb.name)) }
        runOnIdle { snapshot.complete(CasesInfo(listOf(CaseId(1, "First case")), kbName = kb.name)) }

        // Then
        onNodeWithText("Second comment.").assertExists()
        onNodeWithText("First case").assertDoesNotExist()
    }
}
