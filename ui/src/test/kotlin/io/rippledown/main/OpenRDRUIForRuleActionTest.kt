package io.rippledown.main

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.casecontrol.requireCaseSelectorToBeDisplayed
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import io.rippledown.model.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class OpenRDRUIForRuleActionTest {
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
    fun `should show rule action to add a comment`() = runTest {
        val addedComment = "Go to Bondi"
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
            addNewComment(addedComment)

            //Then
            requireLeftInformationMessage("$ADDING$addedComment")
        }
    }

//    @Test
    fun `should show rule action to replace a comment`() = runTest {
        val originalComment = "Go to Bondi"
        val replacementComment = "Go to Malabar"
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf(originalComment))
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
            clickReplaceCommentMenu()
            replaceComment(originalComment, replacementComment)

            //Then
            requireLeftInformationMessage("$REPLACING$originalComment$BY$replacementComment")
        }
    }

//    @Test
    fun `should show rule action to remove a comment`() = runTest {
        val originalComment = "Go to Bondi"
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createCaseWithInterpretation(caseId.name, caseId.id, listOf(originalComment))
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
            clickRemoveCommentMenu()
            removeComment(originalComment)

            //Then
            requireLeftInformationMessage("$REMOVING$originalComment")
        }
    }
}