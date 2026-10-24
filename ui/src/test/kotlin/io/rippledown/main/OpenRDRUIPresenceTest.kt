package io.rippledown.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.kb.PRESENCE_DESCRIPTION
import io.rippledown.model.*
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.voice.VoiceRecognition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalTestApi::class)
class OpenRDRUIPresenceTest {
    private val kb = KBInfo("clinic", "Clinic")
    private lateinit var api: Api
    private lateinit var handler: Handler
    private val voiceRecognition = mockk<VoiceRecognition>()
    private var presenceChanged: (PresenceInfo) -> Unit = {}
    private var chatContextChanged: (ChatContextInfo) -> Unit = {}

    @BeforeEach
    fun setUp() {
        every { voiceRecognition.isListening } returns MutableStateFlow(false)
        every { voiceRecognition.isTranscribing } returns MutableStateFlow(false)
        every { voiceRecognition.partialResult } returns MutableStateFlow("")
        api = mockk()
        every { api.userId } returns UserId("Alice")
        coEvery { api.chatContext() } returns ChatContextInfo(kb, null)
        coEvery { api.presence() } returns emptyList()
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { api.waitingCasesInfo() } returns CasesInfo()
        coEvery { api.kbDescription(any()) } returns ""
        coEvery { api.startConversation(any(), any()) } returns ChatResponse("")
        coEvery { api.startWebSocketSession(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            chatContextChanged = arg(5)
            presenceChanged = arg(6)
        }
        handler = mockk()
        every { handler.api } returns api
        every { handler.isClosing } returns { true }
    }

    @Test
    fun `a presence push takes precedence over an older snapshot`() = runComposeUiTest {
        // Given an adopted KB whose presence snapshot is still loading
        val snapshot = CompletableDeferred<List<PresentUser>>()
        coEvery { api.presence() } coAnswers { snapshot.await() }
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voiceRecognition) }
        waitForIdle()
        coVerify(atLeast = 1) { api.presence() }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertDoesNotExist()

        // When a newer push arrives, followed by the older snapshot response
        runOnIdle {
            presenceChanged(PresenceInfo(kb.id, listOf(PresentUser("Alice", false), PresentUser("Carol", true))))
        }
        presence.assertTextEquals("Also here: Carol")
        runOnIdle { snapshot.complete(listOf(PresentUser("Alice", false), PresentUser("Bob", false))) }

        // Then the current presence is retained and self is excluded
        presence.assertTextEquals("Also here: Carol")
        coVerify(exactly = 0) { api.startConversation(any(), any()) }
    }

    @Test
    fun `adoption loads presence and pushes only update the open KB`() = runComposeUiTest {
        // Given an existing conversation adopted without starting it again
        coEvery { api.presence() } returns listOf(PresentUser("Alice", false), PresentUser("Bob", true))

        // When
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voiceRecognition) }

        // Then
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertTextEquals("Also here: Bob")
        coVerify(exactly = 1) { api.presence() }
        coVerify(exactly = 0) { api.startConversation(any(), any()) }

        // When another KB reports a different set of users
        runOnIdle { presenceChanged(PresenceInfo("other-kb", listOf(PresentUser("Carol", false)))) }

        // Then
        presence.assertTextEquals("Also here: Bob")

        // When the open KB reports that only this user remains
        runOnIdle { presenceChanged(PresenceInfo(kb.id, listOf(PresentUser("Alice", true)))) }

        // Then
        presence.assertDoesNotExist()
    }

    @Test
    fun `switching and closing the KB clears presence while its snapshot is pending`() = runComposeUiTest {
        // Given Bob is present on the initial KB
        coEvery { api.presence() } returns listOf(PresentUser("Bob", false))
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voiceRecognition) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertTextEquals("Also here: Bob")

        // When a different KB is adopted and its snapshot has not returned
        val snapshot = CompletableDeferred<List<PresentUser>>()
        coEvery { api.presence() } coAnswers { snapshot.await() }
        runOnIdle { chatContextChanged(ChatContextInfo(KBInfo("other", "Other"), null)) }

        // Then names from the previous KB disappear immediately
        presence.assertDoesNotExist()
        coVerify(exactly = 2) { api.presence() }

        // When a push arrives on the new KB, then it is closed before the snapshot completes
        runOnIdle { presenceChanged(PresenceInfo("other", listOf(PresentUser("Carol", false)))) }
        presence.assertTextEquals("Also here: Carol")
        runOnIdle { chatContextChanged(ChatContextInfo(null, null)) }
        runOnIdle { snapshot.complete(listOf(PresentUser("Dave", false))) }

        // Then the late response cannot repopulate presence
        presence.assertDoesNotExist()
        coVerify(exactly = 2) { api.presence() }
    }

    @Test
    fun `a failed snapshot leaves presence empty and a later push still works`() = runComposeUiTest {
        // Given
        coEvery { api.presence() } throws IllegalStateException("Unavailable")
        setContent { OpenRDRUI(handler, dispatcher = Unconfined, voiceRecognition = voiceRecognition) }
        val presence = onNodeWithContentDescription(PRESENCE_DESCRIPTION)
        presence.assertDoesNotExist()

        // When
        runOnIdle { presenceChanged(PresenceInfo(kb.id, listOf(PresentUser("Bob", false)))) }

        // Then
        presence.assertTextEquals("Also here: Bob")
    }
}
