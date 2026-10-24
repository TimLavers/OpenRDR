package io.rippledown.main

import androidx.compose.ui.test.*
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.ChatTestHook
import io.rippledown.chat.NoticeMessage
import io.rippledown.constants.chat.NOTICE_PREFIX
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.model.change.KbChange
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
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
class OpenRDRUIChangeTest {
    private val kb = KBInfo("clinic", "Clinic")
    private val alice = UserId("Alice")
    private lateinit var api: Api
    private lateinit var handler: Handler
    private val voice = mockk<VoiceRecognition>()
    private var kbChanged: (KbChange) -> Unit = {}
    private var contextChanged: (ChatContextInfo) -> Unit = {}
    private var sessionChanged: (CornerstoneStatus) -> Unit = {}
    private var sessionCompleted: () -> Unit = {}

    @BeforeEach
    fun setUp() {
        ChatTestHook.reset()
        every { voice.isListening } returns MutableStateFlow(false)
        every { voice.isTranscribing } returns MutableStateFlow(false)
        every { voice.partialResult } returns MutableStateFlow("")
        api = mockk()
        every { api.userId } returns alice
        coEvery { api.chatContext() } returns ChatContextInfo(kb, 1L)
        coEvery { api.presence() } returns emptyList()
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(CaseId(1, "First case"), CaseId(2, "Second case")))
        coEvery { api.getCase(1) } returns caseWith("Original comment.")
        coEvery { api.getCase(2) } returns createViewableCaseWithInterpretation("Second case", 2, listOf("Second comment."))
        coEvery { api.kbDescription(any()) } returns "Original description."
        coEvery { api.startConversation(any(), any()) } returns ChatResponse("")
        coEvery { api.startWebSocketSession(any(), any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            sessionChanged = arg(0)
            sessionCompleted = arg(1)
            contextChanged = arg(5)
            kbChanged = arg(7)
        }
        handler = mockk()
        every { handler.api } returns api
        every { handler.isClosing } returns { true }
    }

    private fun caseWith(comment: String) = createViewableCaseWithInterpretation("First case", 1, listOf(comment))

    private fun change(id: Int? = 1, user: String = "Bob", kind: ChangeKind = ChangeKind.RULES_ADDED, kbId: String = kb.id) =
        KbChange(kbId, ChangeRecord(id, user, 1L, kind, "Changed the knowledge base."))

    @Test
    fun `another user's change refreshes the displayed case and adds a notice`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } returns caseWith("Updated comment.")

        // When
        runOnIdle { kbChanged(change()) }

        // Then
        onNodeWithText("Updated comment.").assertExists()
        onNodeWithContentDescription("${NOTICE_PREFIX}0:Bob: Changed the knowledge base.")
            .assertTextEquals("Bob: Changed the knowledge base.")
        ChatTestHook.snapshot(alice).mostRecentBotText shouldBe null
        coVerify(exactly = 2) { api.getCase(1) }
    }

    @ParameterizedTest
    @EnumSource(ChangeKind::class, names = ["RULES_ADDED", "RULES_UNDONE", "DEFINITION_EDITED", "ATTRIBUTE_RENAMED", "CONDITION_RENAMED", "ATTRIBUTE_MOVED"])
    fun `the author's own changes refresh the case without a notice`(kind: ChangeKind) = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } returns caseWith("Updated comment.")

        // When
        runOnIdle { kbChanged(change(user = "Alice", kind = kind)) }

        // Then
        onNodeWithText("Updated comment.").assertExists()
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>() shouldBe emptyList()
        coVerify(exactly = 2) { api.getCase(1) }
    }

    @Test
    fun `changes for another KB are ignored and do not consume its record id`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()

        // When
        runOnIdle { kbChanged(change(kbId = "other")) }

        // Then
        coVerify(exactly = 1) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList shouldBe emptyList()

        // When a different KB uses the same record id
        runOnIdle { kbChanged(change()) }

        // Then
        waitForIdle()
        coVerify(exactly = 2) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 1
    }

    @Test
    fun `changes during rule building are noticed immediately and refreshed once on completion`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } returns caseWith("Updated comment.")
        coEvery { api.kbDescription(any()) } returns "Updated description."
        runOnIdle { sessionChanged(CornerstoneStatus()) }

        // When
        runOnIdle {
            kbChanged(change(1))
            kbChanged(change(2, kind = ChangeKind.ATTRIBUTE_RENAMED))
            kbChanged(change(3, kind = ChangeKind.DESCRIPTION_SET))
        }

        // Then
        waitForIdle()
        coVerify(exactly = 1) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 3
        onNodeWithContentDescription("Knowledge base description").assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Original description.")
        )

        // When
        runOnIdle { sessionCompleted() }

        // Then
        onNodeWithText("Updated comment.").assertExists()
        onNodeWithContentDescription("Knowledge base description").assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Updated description.")
        )
        coVerify(exactly = 2) { api.getCase(1) }

        // When a repeated completion arrives
        runOnIdle { sessionCompleted() }

        // Then
        waitForIdle()
        coVerify(exactly = 2) { api.getCase(1) }
    }

    @Test
    fun `a case switched during a refresh keeps the newly selected case`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        val fetched = CompletableDeferred<ViewableCase?>()
        coEvery { api.getCase(1) } coAnswers { fetched.await() }
        runOnIdle { kbChanged(change()) }

        // When
        runOnIdle { contextChanged(ChatContextInfo(kb, 2L)) }
        onNodeWithText("Second comment.").assertExists()
        runOnIdle { fetched.complete(caseWith("Late comment.")) }

        // Then
        onNodeWithText("Second comment.").assertExists()
        onNodeWithText("Late comment.").assertDoesNotExist()
    }

    @Test
    fun `a KB switched during a refresh rejects results even when case ids match`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        val fetched = CompletableDeferred<ViewableCase?>()
        coEvery { api.getCase(1) } coAnswers { fetched.await() }
        runOnIdle { kbChanged(change()) }
        waitForIdle()
        coEvery { api.getCase(1) } returns caseWith("Other KB comment.")

        // When
        runOnIdle { contextChanged(ChatContextInfo(KBInfo("other", "Other"), 1L)) }
        onNodeWithText("Other KB comment.").assertExists()
        runOnIdle { fetched.complete(caseWith("Late comment.")) }

        // Then
        onNodeWithText("Other KB comment.").assertExists()
        onNodeWithText("Late comment.").assertDoesNotExist()
    }

    @Test
    fun `a missing refreshed case leaves the current case in place`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } returns null

        // When
        runOnIdle { kbChanged(change()) }

        // Then
        onNodeWithText("Original comment.").assertExists()
        coVerify(exactly = 2) { api.getCase(1) }
    }

    @Test
    fun `description changes reload only the description`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.kbDescription(any()) } returns "Updated description."

        // When
        runOnIdle { kbChanged(change(kind = ChangeKind.DESCRIPTION_SET)) }

        // Then
        onNodeWithContentDescription("Knowledge base description").assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Updated description.")
        )
        coVerify(exactly = 1) { api.getCase(1) }
    }

    @Test
    fun `duplicate ids do not cause repeated refreshes or notices`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()

        // When
        runOnIdle { kbChanged(change()) }
        waitForIdle()
        runOnIdle { kbChanged(change()) }

        // Then
        waitForIdle()
        coVerify(exactly = 2) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 1
    }

    @Test
    fun `unpersisted changes without ids are still refreshed and noticed`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()

        // When
        runOnIdle { kbChanged(change(id = null)) }
        waitForIdle()
        runOnIdle { kbChanged(change(id = null)) }

        // Then
        waitForIdle()
        coVerify(exactly = 3) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 2
    }

    @Test
    fun `case deletion notices leave selection and refresh to cases info`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()

        // When
        runOnIdle { kbChanged(change(kind = ChangeKind.CASE_DELETED)) }

        // Then
        onNodeWithText("Original comment.").assertExists()
        coVerify(exactly = 1) { api.getCase(1) }
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 1
    }

    @Test
    fun `a failed refresh keeps the case and does not prevent later changes`() = runComposeUiTest {
        // Given
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voice) }
        onNodeWithText("Original comment.").assertExists()
        coEvery { api.getCase(1) } throws IllegalStateException("Unavailable")

        // When
        runOnIdle { kbChanged(change()) }

        // Then
        onNodeWithText("Original comment.").assertExists()

        // When
        coEvery { api.getCase(1) } returns caseWith("Updated comment.")
        runOnIdle { kbChanged(change(2)) }

        // Then
        onNodeWithText("Updated comment.").assertExists()
        ChatTestHook.snapshot(alice).messageList.filterIsInstance<NoticeMessage>().size shouldBe 2
    }
}
