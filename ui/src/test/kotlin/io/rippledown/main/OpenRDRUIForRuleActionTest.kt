package io.rippledown.main

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.casecontrol.requireCaseSelectorToBeDisplayed
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.chat.clickChatIconToggle
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.interpretation.*
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.test.StandardTestDispatcher
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
        api = mockk<Api>()
        handler = mockk<Handler>()
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should show rule action to add a comment`() = runTest {
        val addedComment = "Go to Bondi"
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createViewableCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            // Given
            waitForCaseToBeShowing(caseName)
            requireCaseSelectorToBeDisplayed()
            clickChangeInterpretationButton()

            // When
            clickAddCommentMenu()
            addNewComment(addedComment)

            // Then
            requireLeftInformationMessage("$ADDING$addedComment")
        }
    }

    @Test
    fun `should disable the ChangeInterpretationButton if the chat is showing`() = runTest {
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createViewableCase(caseId)
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            // Given
            waitForCaseToBeShowing(caseName)

            // When
            clickChatIconToggle()

            // Then
            requireChangeInterpretationIconToBeNotShowing()
        }
    }

    @Test
    fun `should show rule action to replace a comment`() = runTest {
        // Set up test dispatcher and scheduler
        val testDispatcher = StandardTestDispatcher()

        val originalComment = "Go to Bondi"
        val replacementComment = "Go to Malabar"
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createViewableCaseWithInterpretation(caseId.name, caseId.id, listOf(originalComment))
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))

        with(composeTestRule) {
            // Set content with a test dispatcher
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }

            // Given
            waitForCaseToBeShowing(caseName)
            requireCaseSelectorToBeDisplayed()
            clickChangeInterpretationButton()

            // When
            clickReplaceCommentMenu()
            replaceComment(originalComment, replacementComment)

            // Then
            requireLeftInformationMessage("$REPLACING$originalComment$BY$replacementComment")
        }
    }

    @Test
    fun `should show rule action to remove a comment`() = runTest {
        val originalComment = "Go to Bondi"
        val caseName = "case a"
        val caseId = CaseId(id = 1, name = caseName)
        val case = createViewableCaseWithInterpretation(caseId.name, caseId.id, listOf(originalComment))
        coEvery { api.kbList() } returns listOf(KBInfo("kb1"))
        coEvery { api.getCase(1) } returns case
        coEvery { api.waitingCasesInfo() } returns CasesInfo(listOf(caseId))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            // Given
            waitForCaseToBeShowing(caseName)
            requireCaseSelectorToBeDisplayed()
            clickChangeInterpretationButton()

            // When
            clickRemoveCommentMenu()
            removeComment(originalComment)

            // Then
            requireLeftInformationMessage("$REMOVING$originalComment")
        }
    }
}