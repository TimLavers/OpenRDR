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
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class PollForCornerstonesTest {
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
    fun `should show Cornerstones if a rule session is in progress`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() }

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireNamesToBeShowingOnCaseList(caseA)

            //When
            waitForCaseToBeShowing(caseA)

            //Then
            requireChatPanelIsNotDisplayed()
        }
    }

    @Test
    fun `should show the chat panel if the chat toggle is clicked`() = runTest {
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
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireChatPanelIsNotDisplayed()

            //When
            clickChatIconToggle()

            //Then
            requireChatPanelIsDisplayed()
        }
    }

    @Test
    fun `focus should be on the chat user text field if a case is showing and the chat toggle icon has been clicked`() =
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
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }
                requireNamesToBeShowingOnCaseList(caseA)
                waitForCaseToBeShowing(caseA)

                //When
                clickChatIconToggle()

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
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireNamesToBeShowingOnCaseList(caseA)
            waitForCaseToBeShowing(caseA)
            clickChatIconToggle()
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
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing(caseName)
            clickChatIconToggle()

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
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            clickChatIconToggle()

            //When
            waitForCaseToBeShowing(caseName)

            //Then
            coVerify { api.startConversation(id) }
        }
    }

    @Test
    fun `should not start a conversation with the model when a case is selected if the chat panel is not showing`() =
        runTest {
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
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }
                requireChatPanelIsNotDisplayed()

                //When
                waitForCaseToBeShowing(caseName)

                //Then
                coVerify(exactly = 0) { api.startConversation(id) }
            }
        }

    @Test
    fun `should start a new conversation with the model when another case is selected`() = runTest {
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
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            clickChatIconToggle()
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
        coEvery { api.startConversation(id) } returns initialResponse

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //When
            waitForCaseToBeShowing(caseName)
            clickChatIconToggle()

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
        coEvery { api.sendUserMessage(any(), any<Long>()) } returns answer

        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing(caseA)
            clickChatIconToggle()

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
        @Test
        fun `should poll for cornerstones if a rule session is started by the chat`() = runTest {
            val caseA = "case A"
            val caseId1 = CaseId(id = 1, name = caseA)
            val caseIds = listOf(caseId1)
            val bondiComment = "Go to Bondi"
            val answer = "the answer is 42"
            val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
            coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
            coEvery { api.getCase(1) } returns case
            coEvery { api.sendUserMessage(any(), any<Long>()) } returns answer

            with(composeTestRule) {
                //Given
                setContent {
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }

                //When
                waitForCaseToBeShowing(caseA)
                clickChatIconToggle()

                //Then
                val expected = listOf(
                    UserMessage("userMessage"),
                    BotMessage(answer)
                )
                requireChatMessagesShowing(expected)
            }
        }

    }
}