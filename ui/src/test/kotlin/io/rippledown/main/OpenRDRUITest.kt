package io.rippledown.main

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.appbar.*
import io.rippledown.casecontrol.*
import io.rippledown.chat.BotMessage
import io.rippledown.chat.requireChatMessagesShowing
import io.rippledown.chat.requireChatPanelIsDisplayed
import io.rippledown.chat.typeChatMessageAndClickSend
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import io.rippledown.constants.kb.CONFIRM_UNDO_LAST_RULE_TEXT
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
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
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { api.kbList() } returns emptyList()
        coEvery { api.waitingCasesInfo() } returns CasesInfo()
        coEvery { api.startWebSocketSession(any(), any(), any()) } returns Unit
        handler = mockk<Handler>()
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should start a web socket session when composed`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //Then
            coVerify {
                api.startWebSocketSession(
                    updateCornerstoneStatus = any(),
                    ruleSessionCompleted = any(),
                    updateCasesInfo = any()
                )
            }
        }
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
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
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
    fun `should update the case when a response to a user message is received`() = runTest {
        val caseName = "case a"
        val id = 1L
        val caseId = CaseId(id = id, name = caseName)
        val bondiComment = "Go to Bondi"
        val updatedBondiComments = "Go to Bondi. Bring flippers."
        val case = createViewableCaseWithInterpretation(caseName, id, listOf(bondiComment))
        val updatedCase = createViewableCaseWithInterpretation(caseName, id, listOf(updatedBondiComments))
        coEvery { api.getCase(id) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            //Given
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing(caseName)
            coVerify(exactly = 1) { api.getCase(id) }
            requireInterpretation(bondiComment)

            //When
            coEvery { api.getCase(id) } returns updatedCase
            val userMessage = "yes, please add that comment to the interpretation"
            typeChatMessageAndClickSend(userMessage)

            //Then
            coVerify(exactly = 2) { api.getCase(id) }
            requireInterpretation(updatedBondiComments)
        }
    }


    @Test
    fun `should show case list for several cases`() = runTest {
        val caseIds = (1..10).map { i ->
            val caseId = CaseId(id = i.toLong(), name = "case $i")
            coEvery { api.getCase(caseId.id!!) } returns createViewableCase(caseId)
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
        val caseA = createViewableCase(caseId1)
        val caseB = createViewableCase(caseId2)

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
        val case = createViewableCase(caseId1)
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

        val viewableCaseA = createViewableCaseWithInterpretation(
            name = caseA,
            caseId = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createViewableCaseWithInterpretation(
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
    fun `should show cornerstone cases in the case list`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(CaseId(id = 2, name = "c1"))
        coEvery { api.getCase(1) } returns createViewableCase(CaseId(id = 1, name = "p1"))
        coEvery { api.getCase(2) } returns createViewableCase(CaseId(id = 2, name = "c1"))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(
            caseIds = processed,
            cornerstoneCaseIds = cornerstones
        )

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList("p1", "c1")
        }
    }

    @Test
    fun `should select a cornerstone case when clicked`() = runTest {
        val cornerstones = listOf(
            CaseId(id = 1, name = "c1"),
            CaseId(id = 2, name = "c2")
        )
        val case1 = createViewableCase(CaseId(id = 1, name = "c1"))
        val case2 = createViewableCase(CaseId(id = 2, name = "c2"))
        coEvery { api.getCase(1) } returns case1
        coEvery { api.getCase(2) } returns case2
        coEvery { api.waitingCasesInfo() } returns CasesInfo(cornerstoneCaseIds = cornerstones)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("c1")

            selectCaseByName("c2")

            waitForCaseToBeShowing("c2")
        }
    }

    @Test
    fun `should not show the total case count header`() = runTest {
        val caseId = CaseId(id = 1, name = "case a")
        coEvery { api.getCase(1) } returns createViewableCase(caseId)
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            requireNumberOfCasesOnCaseList(1)
            onNodeWithContentDescription(NUMBER_OF_CASES_ID).assertDoesNotExist()
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
                OpenRDRUI(handler, dispatcher = Unconfined)
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
        val case = createViewableCaseWithInterpretation(caseId.name, caseId.id, listOf())
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(any()) } returns case
        val descriptionText = "This is the last rule!"
        val lastRuleDescription = UndoRuleDescription(descriptionText, true)
        coEvery { api.lastRuleDescription() } returns lastRuleDescription
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            undoLastRule(descriptionText)
            coVerify {
                api.undoLastRule()
                api.getCase(1)
            }
        }
    }

    @Test
    fun `should start a new conversation when switching to a different case`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val caseIds = listOf(caseIdA, caseIdB)
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf("Go to Malabar"))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseNameA)
            coVerify(exactly = 1) { api.startConversation(idA) }

            //When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)

            //Then
            coVerify(exactly = 1) { api.startConversation(idB) }
        }
    }

    @Test
    fun `should start a conversation for each case when switching through multiple cases`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseNameC = "case C"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val caseIdC = CaseId(id = 3, name = caseNameC)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val idC = caseIdC.id!!
        val caseIds = listOf(caseIdA, caseIdB, caseIdC)
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf("Go to Malabar"))
        val caseC = createViewableCaseWithInterpretation(caseNameC, idC, listOf("Go to Coogee"))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB
        coEvery { api.getCase(idC) } returns caseC

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseNameA)
            coVerify(exactly = 1) { api.startConversation(idA) }

            //When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)
            coVerify(exactly = 1) { api.startConversation(idB) }

            selectCaseByName(caseNameC)
            waitForCaseToBeShowing(caseNameC)

            //Then
            coVerify(exactly = 1) { api.startConversation(idA) }
            coVerify(exactly = 1) { api.startConversation(idB) }
            coVerify(exactly = 1) { api.startConversation(idC) }
        }
    }

    @Test
    fun `should restart conversation when switching back to a previously viewed case`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val caseIds = listOf(caseIdA, caseIdB)
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf("Go to Malabar"))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseNameA)
            coVerify(exactly = 1) { api.startConversation(idA) }

            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)
            coVerify(exactly = 1) { api.startConversation(idB) }

            //When
            selectCaseByName(caseNameA)
            waitForCaseToBeShowing(caseNameA)

            //Then
            coVerify(exactly = 2) { api.startConversation(idA) }
            coVerify(exactly = 1) { api.startConversation(idB) }
        }
    }

    @Test
    fun `should not restart conversation when sending a message on the same case`() = runTest {
        val caseName = "case A"
        val caseId = CaseId(id = 1, name = caseName)
        val id = caseId.id!!
        val case = createViewableCaseWithInterpretation(caseName, id, listOf("Go to Bondi"))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(id) } returns case

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseName)
            coVerify(exactly = 1) { api.startConversation(id) }

            //When
            typeChatMessageAndClickSend("add a comment")

            //Then
            coVerify(exactly = 1) { api.startConversation(id) }
        }
    }

    @Test
    fun `should show bot message from conversation started on case switch`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val caseIds = listOf(caseIdA, caseIdB)
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf("Go to Malabar"))
        val responseA = "Would you like to add a comment to case A?"
        val responseB = "Would you like to add a comment to case B?"
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB
        coEvery { api.startConversation(idA) } returns ChatResponse(responseA)
        coEvery { api.startConversation(idB) } returns ChatResponse(responseB)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseNameA)
            requireChatMessagesShowing(listOf(BotMessage(responseA)))

            //When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)

            //Then
            requireChatMessagesShowing(listOf(BotMessage(responseA), BotMessage(responseB)))
        }
    }

    @Test
    fun `should start conversation but not show message when response is blank`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseIdA = CaseId(id = 1, name = caseNameA)
        val caseIdB = CaseId(id = 2, name = caseNameB)
        val idA = caseIdA.id!!
        val idB = caseIdB.id!!
        val caseIds = listOf(caseIdA, caseIdB)
        val caseA = createViewableCaseWithInterpretation(caseNameA, idA, listOf("Go to Bondi"))
        val caseB = createViewableCaseWithInterpretation(caseNameB, idB, listOf("Go to Malabar"))
        val responseA = "Would you like to add a comment?"
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(idA) } returns caseA
        coEvery { api.getCase(idB) } returns caseB
        coEvery { api.startConversation(idA) } returns ChatResponse(responseA)
        coEvery { api.startConversation(idB) } returns ChatResponse("")

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given
            waitForCaseToBeShowing(caseNameA)
            requireChatMessagesShowing(listOf(BotMessage(responseA)))

            //When
            selectCaseByName(caseNameB)
            waitForCaseToBeShowing(caseNameB)

            //Then
            coVerify(exactly = 1) { api.startConversation(idB) }
        }
    }

    @Test
    fun `should show Addition diff in InterpretationView when cornerstone status has an Addition diff`() = runTest {
        //Given
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCornerstoneStatus = firstArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")

            //When
            runOnIdle {
                updateCornerstoneStatus!!.invoke(CornerstoneStatus(diff = Addition("Go to Bondi.")))
            }
            waitForIdle()

            //Then
            requireInterpretation("Go to Bondi.")
        }
    }

    @Test
    fun `should show Removal diff in InterpretationView when cornerstone status has a Removal diff`() = runTest {
        //Given
        val bondiComment = "Go to Bondi."
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1, listOf(bondiComment))
        var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCornerstoneStatus = firstArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")
            requireInterpretation(bondiComment)

            //When
            runOnIdle {
                updateCornerstoneStatus!!.invoke(CornerstoneStatus(diff = Removal(bondiComment)))
            }
            waitForIdle()

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show Replacement diff in InterpretationView when cornerstone status has a Replacement diff`() =
        runTest {
        //Given
            val bondiComment = "Go to Bondi."
            val maroubraComment = "Go to Maroubra."
            val caseId = CaseId(id = 1, name = "case A")
            coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
            coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1, listOf(bondiComment))
        var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)? = null
            coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCornerstoneStatus = firstArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")
            requireInterpretation(bondiComment)

            //When
            runOnIdle {
                updateCornerstoneStatus!!.invoke(
                    CornerstoneStatus(diff = Replacement(bondiComment, maroubraComment))
                )
            }
            waitForIdle()

            //Then
            requireInterpretation("$bondiComment $maroubraComment")
        }
    }

    @Test
    fun `should clear the diff in InterpretationView when the rule session completes`() = runTest {
        //Given
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)? = null
        var ruleSessionCompleted: (() -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCornerstoneStatus = firstArg()
            ruleSessionCompleted = secondArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")
            runOnIdle {
                updateCornerstoneStatus!!.invoke(CornerstoneStatus(diff = Addition("Go to Bondi.")))
            }
            waitForIdle()
            requireInterpretation("Go to Bondi.")

            //When
            runOnIdle {
                ruleSessionCompleted!!.invoke()
            }
            waitForIdle()

            //Then
            requireInterpretation("")
        }
    }

    @Test
    fun `should not show diff in InterpretationView when cornerstone status has no diff`() = runTest {
        //Given
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCornerstoneStatus = firstArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")

            //When
            runOnIdle {
                updateCornerstoneStatus!!.invoke(CornerstoneStatus(diff = null))
            }
            waitForIdle()

            //Then
            requireInterpretation("")
        }
    }

    @Test
    fun `should fetch initial casesInfo on startup`() = runTest {
        //Given
        val caseId = CaseId(id = 1, name = "case A")
        val initialCasesInfo = CasesInfo(listOf(caseId))
        coEvery { api.waitingCasesInfo() } returns initialCasesInfo
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)

        with(composeTestRule) {
            //When
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //Then
            waitForCaseToBeShowing("case A")
            coVerify { api.waitingCasesInfo() }
        }
    }

    @Test
    fun `should update case list when CasesInfo is pushed via WebSocket`() = runTest {
        //Given
        val caseIdA = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseIdA))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        var updateCasesInfo: ((CasesInfo) -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCasesInfo = thirdArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")
            requireNamesToBeShowingOnCaseList("case A")

            //When - simulate server pushing new CasesInfo with an additional case
            val caseIdB = CaseId(id = 2, name = "case B")
            coEvery { api.getCase(2) } returns createViewableCaseWithInterpretation("case B", 2)
            runOnIdle {
                updateCasesInfo?.invoke(CasesInfo(listOf(caseIdA, caseIdB)))
            }

            //Then
            requireNamesToBeShowingOnCaseList("case A", "case B")
        }
    }

    @Test
    fun `should show no cases when WebSocket pushes empty CasesInfo`() = runTest {
        //Given
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        var updateCasesInfo: ((CasesInfo) -> Unit)? = null
        coEvery { api.startWebSocketSession(any(), any(), any()) } coAnswers {
            updateCasesInfo = thirdArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing("case A")

            //When - simulate server pushing empty CasesInfo (all cases deleted)
            runOnIdle {
                updateCasesInfo?.invoke(CasesInfo())
            }

            //Then
            requireCaseSelectorNotToBeDisplayed()
        }
    }

    @Test
    fun `should show chat panel when there are no cases`() = runTest {
        //Given - start with empty CasesInfo
        coEvery { api.waitingCasesInfo() } returns CasesInfo()

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //Then - chat panel should still be visible
            requireChatPanelIsDisplayed()
        }
    }

    @Test
    fun `should refetch casesInfo when the selected KB changes`() = runTest {
        //Given two KBs on the server. Initially KB-A is current with one case.
        val kbA = KBInfo("id_a", "KB_A")
        val kbB = KBInfo("id_b", "KB_B")
        val caseIdA = CaseId(id = 1, name = "case A")
        val caseIdB = CaseId(id = 2, name = "case B")
        coEvery { api.kbList() } returns listOf(kbA, kbB)
        coEvery { api.selectKB("id_b") } returns kbB
        var casesForCurrentKb = CasesInfo(listOf(caseIdA))
        coEvery { api.waitingCasesInfo() } coAnswers { casesForCurrentKb }
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)
        coEvery { api.getCase(2) } returns createViewableCaseWithInterpretation("case B", 2)

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given - initial load shows KB-A's case
            waitForCaseToBeShowing("case A")
            coVerify(atLeast = 1) { api.waitingCasesInfo() }

            //When - user selects KB-B from the dropdown; server state now reflects KB-B's cases
            casesForCurrentKb = CasesInfo(listOf(caseIdB))
            clickDropdown()
            onNodeWithContentDescription("${KB_INFO_ITEM}KB_B").performClick()

            //Then - casesInfo was refetched and KB-B's case is shown
            waitForCaseToBeShowing("case B")
            coVerify { api.selectKB("id_b") }
            coVerify(atLeast = 2) { api.waitingCasesInfo() }
        }
    }

    @Test
    fun `should update case list after switching KBs to reflect new KB's cases`() = runTest {
        //Given
        val kbA = KBInfo("id_a", "KB_A")
        val kbB = KBInfo("id_b", "KB_B")
        val caseA1 = CaseId(id = 1, name = "a-1")
        val caseA2 = CaseId(id = 2, name = "a-2")
        val caseB1 = CaseId(id = 3, name = "b-1")
        coEvery { api.kbList() } returns listOf(kbA, kbB)
        coEvery { api.selectKB("id_b") } returns kbB
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("a-1", 1)
        coEvery { api.getCase(2) } returns createViewableCaseWithInterpretation("a-2", 2)
        coEvery { api.getCase(3) } returns createViewableCaseWithInterpretation("b-1", 3)
        var casesForCurrentKb = CasesInfo(listOf(caseA1, caseA2))
        coEvery { api.waitingCasesInfo() } coAnswers { casesForCurrentKb }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            //Given - KB-A cases are displayed
            waitForCaseToBeShowing("a-1")
            requireNamesToBeShowingOnCaseList("a-1", "a-2")

            //When - user selects KB-B, which has different cases
            casesForCurrentKb = CasesInfo(listOf(caseB1))
            clickDropdown()
            onNodeWithContentDescription("${KB_INFO_ITEM}KB_B").performClick()

            //Then - list now shows only KB-B's case
            waitForCaseToBeShowing("b-1")
            requireNamesToBeShowingOnCaseList("b-1")
        }
    }

    @Test
    fun `should refetch casesInfo on initial load`() = runTest {
        //Given
        val kbInfo = KBInfo("id_a", "KB_A")
        val caseId = CaseId(id = 1, name = "case A")
        coEvery { api.kbList() } returns listOf(kbInfo)
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.getCase(1) } returns createViewableCaseWithInterpretation("case A", 1)

        with(composeTestRule) {
            //When
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            //Then - waitingCasesInfo is called at least once for the initial KB
            waitForCaseToBeShowing("case A")
            coVerify(atLeast = 1) { api.waitingCasesInfo() }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeContentTestRule.undoLastRule(descriptionText: String) {
        clickEditKbDropdown()
        assertEditKbDescriptionMenuItemIsShowing()
        clickUndoLastRuleMenuItem()
        waitUntilExactlyOneExists(hasText(descriptionText), timeoutMillis = 5000)
        waitForIdle()
        clickUndoLastRule()
        waitUntilExactlyOneExists(hasText(CONFIRM_UNDO_LAST_RULE_TEXT), timeoutMillis = 5000)
        waitForIdle()
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
    coEvery { api.cornerstoneStatus() } returns null
    coEvery { api.getCase(any()) } returns createViewableCaseWithInterpretation("case A", 1, listOf("Go to Bondi"))
    coEvery { api.sendUserMessage(any(), any()) } returns ChatResponse("The answer is 42")

    applicationFor {
        OpenRDRUI(handler, dispatcher = Unconfined)
    }
}
