package io.rippledown.main

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.appbar.assertKbNameIs
import io.rippledown.casecontrol.*
import io.rippledown.constants.interpretation.ADD_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REMOVE_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REPLACED_COMMENT_PREFIX
import io.rippledown.constants.interpretation.REPLACEMENT_COMMENT_PREFIX
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.interpretation.*
import io.rippledown.model.*
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.rule.clickCancelRuleButton
import io.rippledown.rule.clickFinishRuleButton
import io.rippledown.utils.applicationFor
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
        api = mockk<Api>(relaxed = true)
        handler = mockk<Handler>(relaxed = true)
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should show OpenRDR UI`() = runTest {
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
            }
            onNodeWithTag(testTag = APPLICATION_BAR_ID).assertExists()
        }
    }

    @Test
    fun `should show the first project if there is one`() = runTest {
        coEvery { api.kbList() } returns listOf(KBInfo("Bondi"), KBInfo("Malabar"))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
            id = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            id = 2,
            conclusionTexts = listOf(malabarComment)
        )
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(caseId1.id!!) } returns viewableCaseA
        coEvery { api.getCase(caseId2.id!!) } returns viewableCaseB

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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
            id = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            id = 2,
            conclusionTexts = listOf(malabarComment)
        )
        val normalTSH = EpisodicCondition(null, Attribute(1, "tsh"), Normal, Current)
        val normalFT3 = EpisodicCondition(null, Attribute(2, "ft3"), Normal, Current)

        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(caseId1.id!!) } returns viewableCaseA
        coEvery { api.getCase(caseId2.id!!) } returns viewableCaseB
        coEvery { api.conditionHints(caseId1.id!!) } returns ConditionList(listOf(NonEditableSuggestedCondition(normalTSH)))
        coEvery { api.conditionHints(caseId2.id!!) } returns ConditionList(listOf(NonEditableSuggestedCondition(normalFT3)))

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
    fun `should call handler to widen the window when a cornerstone case is shown`() = runTest {
        val caseName = "case a"
        val cornerstoneName = "case b"
        val caseId = CaseId(id = 1, name = caseName)
        val cornerstoneId = CaseId(id = 2, name = cornerstoneName)
        val case = createCase(caseId)
        val cornerstone = createCase(cornerstoneId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstone
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstone
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        coEvery { api.startRuleSession(any()) } returns CornerstoneStatus(cornerstone, 0, 1)
        coEvery { api.selectCornerstone(any()) } returns cornerstone
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
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

            //Then
            coVerify { handler.showingCornerstone(false) }
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
                OpenRDRUI(handler)
            }

            //Then
            coVerify { api.allConclusions() }
        }
    }

    @Test
    fun `the dialog to add a comment to a case with no given comments should show all available comments`() = runTest {
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
                OpenRDRUI(handler)
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
                OpenRDRUI(handler)
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
                    OpenRDRUI(handler)
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
                OpenRDRUI(handler)
            }

            //When
            clickChangeInterpretationButton()
            clickRemoveCommentMenu()

            //Then
            requireCommentOptionsToBeDisplayed(REMOVE_COMMENT_PREFIX, listOf(commentA))
        }
    }

}

fun main() {

    val caseIds = (1..100).map { i ->
        CaseId(id = i.toLong(), name = "case $i")
    }
    val handler = mockk<Handler>(relaxed = true)
    val api = mockk<Api>(relaxed = true)
    coEvery { handler.api } returns api
    coEvery { handler.isClosing() } returns false
    coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
    coEvery { api.getCase(any()) } returns createCaseWithInterpretation("case A", 1, listOf("Go to Bondi"))

    applicationFor {
        OpenRDRUI(handler)
    }
}
