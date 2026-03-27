package io.rippledown.main

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.casecontrol.requireNamesToBeShowingOnCaseList
import io.rippledown.casecontrol.selectCaseByName
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.chat.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class OpenRDRUIWithChatTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: Handler
    lateinit var api: Api

    @Before
    fun setUp() {
        api = mockk<Api>()
        handler = mockk<Handler>()
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should show the chat panel by default`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            requireNamesToBeShowingOnCaseList(caseA)

            //When
            waitForCaseToBeShowing(caseA)

            //Then
            requireChatPanelIsDisplayed()
        }
    }

    @Test
    fun `focus should be on the chat user text field if a case is showing`() =
        runTest {
            val caseA = "case A"
            val caseId1 = CaseId(id = 1, name = caseA)
            val caseIds = listOf(caseId1)
            val bondiComment = "Go to Bondi"
            val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
            coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
            coEvery { api.getCase(1) } returns case

            with(composeTestRule) {
                //Given
                setContent {
                    OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
                }
                requireNamesToBeShowingOnCaseList(caseA)

                //When
                waitForCaseToBeShowing(caseA)

                //Then
                requireUserTextFieldFocused()
            }
        }

    @Test
    fun `focus should remain on the chat user text field if a second case is selected`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"
        val case1 = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        val case2 = createViewableCaseWithInterpretation(caseB, 2, listOf(malabarComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case1
        coEvery { api.getCase(2) } returns case2

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            requireNamesToBeShowingOnCaseList(caseA)
            waitForCaseToBeShowing(caseA)
            requireUserTextFieldFocused()

            //When
            selectCaseByName(caseB)
            waitForCaseToBeShowing(caseB)

            //Then
            waitForIdle()
            requireUserTextFieldFocused()
        }
    }

    @Test
    fun `should call the api to send a message to the model when a user chat message is entered`() = runTest {
        val caseName = "case A"
        val caseId = CaseId(id = 1234, name = caseName)
        val id = caseId.id!!
        val caseIds = listOf(caseId)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseName, id, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(id) } returns case

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseName)

            //When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            //Then
            coVerify { api.sendUserMessage(userMessage, id) }
        }
    }

    @Test
    fun `should start a conversation with the model when a case is selected`() = runTest {
        val caseName = "case A"
        val caseId = CaseId(id = 1234, name = caseName)
        val id = caseId.id!!
        val caseIds = listOf(caseId)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseName, id, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(id) } returns case

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }

            //When
            waitForCaseToBeShowing(caseName)

            //Then
            coVerify { api.startConversation(id) }
        }
    }

    @Test
    fun `should start a new conversation with the model when browsing to another case`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1234, name = caseNameA)
        val caseIdB = CaseId(id = 5678, name = caseNameB)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val caseIds = listOf(caseIdA, caseIdB)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf(bondiComment))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf(malabarComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseNameA)

            //When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)

            //Then
            coVerify(exactly = 1) { api.startConversation(idA) }
            coVerify(exactly = 1) { api.startConversation(idB) }
        }
    }

    @Test
    fun `should not show a duplicate bot message when switching cases with the same response`() = runTest {
        // Given
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val caseIds = listOf(caseIdA, caseIdB)
        val caseA = createViewableCaseWithInterpretation(caseNameA, 1, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, 2, listOf("Go to Malabar"))
        val sameResponse = "Would you like to add a comment to the report?"
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns caseA
        coEvery { api.getCase(2) } returns caseB
        coEvery { api.startConversation(1) } returns ChatResponse(sameResponse)
        coEvery { api.startConversation(2) } returns ChatResponse(sameResponse)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseNameA)
            requireChatMessagesShowing(listOf(BotMessage(sameResponse)))

            // When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)

            // Then - only one message, not duplicated
            requireChatMessagesShowing(listOf(BotMessage(sameResponse)))
        }
    }

    @Test
    fun `should update the chat panel with the response from the model when a new conversation is started`() = runTest {
        val caseName = "case A"
        val caseId = CaseId(id = 1234, name = caseName)
        val id = caseId.id!!
        val caseIds = listOf(caseId)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseName, id, listOf(bondiComment))
        val initialResponse = "the answer is 42"
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(id) } returns case
        coEvery { api.startConversation(id) } returns ChatResponse(initialResponse)

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }

            //When
            waitForCaseToBeShowing(caseName)

            //Then
            requireChatMessagesShowing(listOf(BotMessage(initialResponse)))
        }
    }

    @Test
    fun `should update the chat panel when a response to a user message is received`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val answer = "the answer is 42"
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case
        coEvery { api.sendUserMessage(any(), any<Long>()) } returns ChatResponse(answer)

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseA)

            //When
            val userMessage = "add a comment"
            typeChatMessageAndClickSend(userMessage)

            //Then
            val expected = listOf(
                UserMessage(userMessage),
                BotMessage(answer)
            )
            requireChatMessagesShowing(expected)
        }
    }
}

fun main() {
    val caseIds = (1..100).map { i ->
        CaseId(id = i.toLong(), name = "case $i")
    }
    val caseA = createViewableCaseWithInterpretation("case A", 1, listOf("Go to Bondi"))
    val caseB = createViewableCaseWithInterpretation("case B", 2, listOf("Go to Malabar"))
    val handler = mockk<Handler>()
    val api = mockk<Api>()
    coEvery { handler.api } returns api
    coEvery { handler.isClosing() } returns false
    coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
//    coEvery { api.cornerstoneStatus() } returns CornerstoneStatus(caseB, 0, 42)
    coEvery { api.getCase(any()) } returns caseA
    coEvery { api.sendUserMessage(any(), any()) } returns ChatResponse("The answer is 42")

    applicationFor {
        OpenRDRUI(handler, dispatcher = Unconfined)
    }
}
