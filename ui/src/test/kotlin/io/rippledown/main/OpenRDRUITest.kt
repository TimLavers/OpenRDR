package io.rippledown.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.appbar.*
import io.rippledown.casecontrol.*
import io.rippledown.chat.*
import io.rippledown.constants.interpretation.ADD_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REMOVE_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REPLACED_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REPLACEMENT_COMMENT_PREFIX
import io.rippledown.constants.kb.CONFIRM_UNDO_LAST_RULE_TEXT
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.interpretation.*
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.rule.clickCancelRuleButton
import io.rippledown.rule.clickFinishRuleButton
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createCase
import io.rippledown.utils.createCaseWithInterpretation
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test


class OpenRDRUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: Handler
    lateinit var api: Api

    @Before
    fun setUp() {
        api = mockk<Api>()
        handler = mockk<Handler>()
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should show OpenRDR UI`() = runTest {
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            onNodeWithTag(testTag = APPLICATION_BAR_ID).assertExists()
        }
    }

    @Test
    fun `should show the first project if there is one`() = runTest {
        coEvery { api.kbList() } returns listOf(KBInfo("Bondi"), KBInfo("Malabar"))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            assertKbNameIs("Bondi")
        }
    }

    @Test
    fun `should show the interpretation of the first case`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)

            //When
            waitForCaseToBeShowing(caseA)

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should hide the chat panel by default`() = runTest {
        val caseA = "case A"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case

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
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
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
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
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
        val case1 = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        val case2 = createCaseWithInterpretation(caseB, 2, listOf(malabarComment))
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
        val case = createCaseWithInterpretation(caseName, id, listOf(bondiComment))
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
        val case = createCaseWithInterpretation(caseName, id, listOf(bondiComment))
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
            val case = createCaseWithInterpretation(caseName, id, listOf(bondiComment))
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
        val caseA = createCaseWithInterpretation(caseNameA, idA, listOf(bondiComment))
        val caseB = createCaseWithInterpretation(caseNameB, idB, listOf(malabarComment))
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
        val case = createCaseWithInterpretation(caseName, id, listOf(bondiComment))
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
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
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
    }

    @Test
    fun `should update the case when a response to a user message is received`() = runTest {
        val caseName = "case a"
        val id = 1L
        val caseId = CaseId(id = id, name = caseName)
        val bondiComment = "Go to Bondi"
        val updatedBondiComments = "Go to Bondi. Bring flippers."
        val case = createCaseWithInterpretation(caseName, id, listOf(bondiComment))
        val updatedCase = createCaseWithInterpretation(caseName, id, listOf(updatedBondiComments))
        coEvery { api.getCase(id) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing(caseName)
            coVerify(exactly = 2) { api.getCase(id) }
            requireInterpretation(bondiComment)
            clickChatIconToggle()

            //When
            coEvery { api.getCase(id) } returns updatedCase
            val userMessage = "yes, please add that comment to the interpretation"
            typeChatMessageAndClickSend(userMessage)

            //Then
            coVerify(exactly = 3) { api.getCase(id) }
            requireInterpretation(updatedBondiComments)
        }
    }


    @Test
    fun `should show case list for several cases`() = runTest {
        val caseIds = (1..10).map { i ->
            val caseId = CaseId(id = i.toLong(), name = "case $i")
            coEvery { api.getCase(caseId.id!!) } returns createCase(caseId)
            caseId
        }
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)

        val caseName1 = "case 1"
        val caseName10 = "case 10"
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName1)

            //When
            selectCaseByName(caseName10)

            //Then
            waitForCaseToBeShowing(caseName10)
        }
    }

    @Test
    fun `should show a case when its case name is clicked`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseNameC = "case C"
        val caseId1 = CaseId(id = 1, name = caseNameA)
        val caseId2 = CaseId(id = 2, name = caseNameB)
        val caseId3 = CaseId(id = 3, name = caseNameC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val caseA = createCase(caseId1)
        val caseB = createCase(caseId2)

        coEvery { api.getCase(1) } returns caseA
        coEvery { api.getCase(2) } returns caseB
        coEvery { api.waitingCasesInfo() } returns CasesInfo(threeCaseIds)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            requireNumberOfCasesOnCaseList(3)
            requireNamesToBeShowingOnCaseList(caseNameA, caseNameB, caseNameC)

            //When
            selectCaseByName(caseNameB)

            //Then
            waitForCaseToBeShowing(caseNameB)

        }
    }

    @Test
    fun `should list case names`() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        val case = createCase(caseId1)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(twoCaseIds)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
        }
    }

    @Test
    fun `should update the interpretation when a case is selected`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"

        val viewableCaseA = createCaseWithInterpretation(
            name = caseA,
            caseId = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            caseId = 2,
            conclusionTexts = listOf(malabarComment)
        )
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(caseId1.id!!) } returns viewableCaseA
        coEvery { api.getCase(caseId2.id!!) } returns viewableCaseB

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
            waitForCaseToBeShowing(caseA)
            requireInterpretation(bondiComment)

            //When
            selectCaseByName(caseB)

            //Then
            waitForCaseToBeShowing(caseB)
            requireInterpretation(malabarComment)
        }
    }

    @Test
    fun `should update the condition hints when a case is selected`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"

        val viewableCaseA = createCaseWithInterpretation(
            name = caseA,
            caseId = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            caseId = 2,
            conclusionTexts = listOf(malabarComment)
        )
        val normalTSH = EpisodicCondition(null, Attribute(1, "tsh"), Normal, Current)
        val normalFT3 = EpisodicCondition(null, Attribute(2, "ft3"), Normal, Current)

        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(caseId1.id!!) } returns viewableCaseA
        coEvery { api.getCase(caseId2.id!!) } returns viewableCaseB
        coEvery { api.conditionHints(caseId1.id!!) } returns ConditionList(
            listOf(
                NonEditableSuggestedCondition(
                    normalTSH
                )
            )
        )
        coEvery { api.conditionHints(caseId2.id!!) } returns ConditionList(
            listOf(
                NonEditableSuggestedCondition(
                    normalFT3
                )
            )
        )

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
            waitForCaseToBeShowing(caseA)
            coVerify { api.conditionHints(caseId1.id!!) }

            //When
            selectCaseByName(caseB)

            //Then
            coVerify { api.conditionHints(caseId2.id!!) }
        }
    }

    @Test
    fun `should not show case selector when a rule session is started`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            requireCaseSelectorToBeDisplayed()
            clickChangeInterpretationButton()

            //When
            clickAddCommentMenu()
            addNewComment("Go to Bondi")

            //Then
            requireCaseSelectorNotToBeDisplayed()
        }
    }

    @Test
    fun `should show cornerstone view`() = runTest {
        val caseName = "case a"
        val cornerstoneName = "case b"
        val caseId = CaseId(id = 1, name = caseName)
        val cornerstoneId = CaseId(id = 2, name = cornerstoneName)
        val case = createCase(caseId)
        val cornerstone = createCase(cornerstoneId)
        val cornerstoneStatus = CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstoneStatus
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            coVerify { handler.showingCornerstone(false) }
            clickChangeInterpretationButton()

            //When
            clickAddCommentMenu()
            addNewComment("Go to Bondi")

            //Then
            coVerify { handler.showingCornerstone(true) }
        }
    }

    @Test
    fun `should call handler to stop showing cornerstone when a rule session is cancelled`() = runTest {
        val caseName = "case a"
        val cornerstoneName = "case b"
        val caseId = CaseId(id = 1, name = caseName)
        val cornerstoneId = CaseId(id = 2, name = cornerstoneName)
        val case = createCase(caseId)
        val cornerstone = createCase(cornerstoneId)
        val cornerstoneStatus = CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstoneStatus
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            addNewComment("Go to Bondi")
            coVerify { handler.showingCornerstone(true) }

            //When
            clickCancelRuleButton()

            //Then
            coVerify { handler.showingCornerstone(false) }
        }
    }

    @Test
    fun `should call handler to cancel the rule session when a rule session is cancelled`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
        coEvery { api.cancelRuleSession() } returns HttpStatusCode.OK
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            addNewComment("Go to Bondi")

            //When
            clickCancelRuleButton()

            //Then
            coVerify { api.cancelRuleSession() }
        }
    }

    @Test
    fun `should call handler to stop showing cornerstone when a rule session is finished`() = runTest {
        val caseName = "case a"
        val cornerstoneName = "case b"
        val caseId = CaseId(id = 1, name = caseName)
        val cornerstoneId = CaseId(id = 2, name = cornerstoneName)
        val case = createCase(caseId)
        val cornerstone = createCase(cornerstoneId)
        val cornerstoneStatus = CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstoneStatus
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            addNewComment("Go to Bondi")
            waitForIdle()
            coVerify { handler.showingCornerstone(true) }

            //When
            clickFinishRuleButton()
            waitForIdle()

            //Then
            coVerify { handler.showingCornerstone(false) }
        }
    }

    @Test
    fun `should hide the change interpretation icon when a rule session is started to add a comment`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
        coEvery { api.selectCornerstone(any()) } returns CornerstoneStatus()
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            requireChangeInterpretationIconToBeShowing()
            clickChangeInterpretationButton()
            clickAddCommentMenu()

            //When
            addNewComment("Go to Bondi")

            //Then
            requireChangeInterpretationIconToBeNotShowing()
        }
    }

    @Test
    fun `should hide the change interpretation icon when a rule session is started to remove a comment`() =
        runTest {
            //Given
            val caseName = "case a"
            val id = 1L
            val caseId = CaseId(id = id, name = caseName)
            val bondiComment = "Go to Bondi"
            val case = createCaseWithInterpretation(caseName, caseId = id, conclusionTexts = listOf(bondiComment))
            coEvery { api.getCase(1) } returns case
            coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
            coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
            coEvery { api.selectCornerstone(any()) } returns CornerstoneStatus()
            with(composeTestRule) {
                setContent {
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }
                //Given
                waitForCaseToBeShowing(caseName)
                requireChangeInterpretationIconToBeShowing()
                clickChangeInterpretationButton()
                clickRemoveCommentMenu()

                //When
                removeComment(bondiComment)

                //Then
                requireChangeInterpretationIconToBeNotShowing()
            }
        }

    @Test
    fun `should hide the change interpretation icon when a rule session is started to replace a comment`() =
        runTest {
            val caseName = "case a"
            val id = 1L
            val caseId = CaseId(id = id, name = caseName)
            val bondiComment = "Go to Bondi"
            val malabarComment = "Go to Malabar"
            val case = createCaseWithInterpretation(caseName, caseId = id, conclusionTexts = listOf(bondiComment))
            coEvery { api.getCase(1) } returns case
            coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
            coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
            coEvery { api.selectCornerstone(any()) } returns CornerstoneStatus()
            with(composeTestRule) {
                setContent {
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }
                //Given
                waitForCaseToBeShowing(caseName)
                requireChangeInterpretationIconToBeShowing()
                clickChangeInterpretationButton()
                clickReplaceCommentMenu()

                //When
                replaceComment(bondiComment, malabarComment)

                //Then
                requireChangeInterpretationIconToBeNotShowing()
            }
        }

    @Test
    fun `should re-show the change interpretation icon when a rule session is committed`() = runTest {
        val caseName = "case a"
        val id = 1L
        val caseId = CaseId(id = id, name = caseName)
        val case = createCaseWithInterpretation(caseName, id, listOf("Go to Coogee"))
        coEvery { api.getCase(id) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
        coEvery { api.commitSession(any()) } returns case
        coEvery { api.selectCornerstone(any()) } returns CornerstoneStatus()
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            addNewComment("Go to Bondi")
            requireChangeInterpretationIconToBeNotShowing()

            //When
            clickFinishRuleButton()

            //Then
            requireChangeInterpretationIconToBeShowing()
        }
    }

    @Test
    fun `should re-show the change interpretation icon when a rule session is cancelled`() = runTest {
        val caseName = "case a"
        val id = 1L
        val caseId = CaseId(id = id, name = caseName)
        val case = createCaseWithInterpretation(caseName, id, listOf("Go to Coogee"))
        coEvery { api.getCase(id) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus()
        coEvery { api.commitSession(any()) } returns case
        coEvery { api.selectCornerstone(any()) } returns CornerstoneStatus()
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            clickChangeInterpretationButton()
            clickAddCommentMenu()
            addNewComment("Go to Bondi")
            requireChangeInterpretationIconToBeNotShowing()

            //When
            clickCancelRuleButton()

            //Then
            requireChangeInterpretationIconToBeShowing()
        }
    }

    @Test
    fun `should call handler to retrieve all comments`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //Then
            coVerify { api.allConclusions() }
        }
    }

    @Test
    fun `the dialog to add a comment to a case with no given comments should show all available comments`() =
        runTest {
            val caseName = "case a"
            val caseId = CaseId(id = 1, name = caseName)
            val case = createCase(caseId)
            coEvery { api.getCase(1) } returns case
            coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))

            val commentA = "A"
            val commentB = "B"
            val conclusionA = Conclusion(1, commentA)
            val conclusionB = Conclusion(2, commentB)
            coEvery { api.allConclusions() } returns setOf(conclusionA, conclusionB)
            with(composeTestRule) {
                //Given
                setContent {
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }

                //When
                clickChangeInterpretationButton()
                clickAddCommentMenu()

                //Then
                requireCommentOptionsToBeDisplayed(ADD_COMMENT_PREFIX, listOf(commentA, commentB))
            }
        }

    @Test
    fun `the dialog to add a comment should not show any comment already given for the case`() = runTest {
        val commentA = "A"
        val commentB = "B"
        val conclusionA = Conclusion(1, commentA)
        val conclusionB = Conclusion(2, commentB)
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf(commentA))
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.allConclusions() } returns setOf(conclusionA, conclusionB)
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //When
            clickChangeInterpretationButton()
            clickAddCommentMenu()

            //Then
            requireCommentOptionsToBeDisplayed(ADD_COMMENT_PREFIX, listOf(commentB))
        }
    }

    @Test
    fun `the dialog to replace a comment should not show any comment already given for the case as a replacement`() =
        runTest {
            val commentA = "A"
            val commentB = "B"
            val conclusionA = Conclusion(1, commentA)
            val conclusionB = Conclusion(2, commentB)
            val caseName = "case a"
            val caseId = CaseId(id = 1, name = caseName)
            val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf(commentA))
            coEvery { api.getCase(1) } returns case
            coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
            coEvery { api.allConclusions() } returns setOf(conclusionA, conclusionB)
            with(composeTestRule) {
                //Given
                setContent {
                    OpenRDRUI(handler, dispatcher = Unconfined)
                }

                //When
                clickChangeInterpretationButton()
                clickReplaceCommentMenu()

                //Then
                requireCommentOptionsToBeDisplayed(REPLACED_COMMENT_PREFIX, listOf(commentA))
                requireCommentOptionsToBeDisplayed(REPLACEMENT_COMMENT_PREFIX, listOf(commentB))
            }
        }

    @Test
    fun `the dialog to remove a comment should only show the comments already given for the case`() = runTest {
        val commentA = "A"
        val commentB = "B"
        val conclusionA = Conclusion(1, commentA)
        val conclusionB = Conclusion(2, commentB)
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf(commentA))
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.allConclusions() } returns setOf(conclusionA, conclusionB)
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //When
            clickChangeInterpretationButton()
            clickRemoveCommentMenu()

            //Then
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(commentA))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `when a rule is undone and there is no current case, the case is not refreshed`() = runTest {
        val descriptionText = "This is the last rule!"
        val lastRuleDescription = UndoRuleDescription(descriptionText, true)

        coEvery { api.lastRuleDescription() } returns lastRuleDescription
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
            }
            undoLastRule(descriptionText)
            coVerify {
                api.undoLastRule()
            }
            coVerify(exactly = 0) {
                api.getCase(any())
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `when a rule is undone the current case should be refreshed`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf())
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(any()) } returns case
        val descriptionText = "This is the last rule!"
        val lastRuleDescription = UndoRuleDescription(descriptionText, true)
        coEvery { api.lastRuleDescription() } returns lastRuleDescription
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
            }
            undoLastRule(descriptionText)
            coVerify {
                api.undoLastRule()
                api.getCase(1)
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeContentTestRule.undoLastRule(descriptionText: String) {
        clickEditKbDropdown()
        assertEditKbDescriptionMenuItemIsShowing()
        clickUndoLastRuleMenuItem()
        waitUntilExactlyOneExists(hasText(descriptionText))
        clickUndoLastRule()
        waitUntilExactlyOneExists(hasText(CONFIRM_UNDO_LAST_RULE_TEXT))
        assertUndoLastRuleButtonIsNotShowing()
        clickUndoLastRuleConfirmationYesButton()
    }
}

fun main() {
    val caseIds = (1..100).map { i ->
        CaseId(id = i.toLong(), name = "case $i")
    }
    val handler = mockk<Handler>()
    val api = mockk<Api>()
    coEvery { handler.api } returns api
    coEvery { handler.isClosing() } returns false
    coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
    coEvery { api.getCase(any()) } returns createCaseWithInterpretation("case A", 1, listOf("Go to Bondi"))
    coEvery { api.sendUserMessage(any(), any()) } returns "The answer is 42"

    applicationFor {
        OpenRDRUI(handler, dispatcher = Unconfined)
    }
}
