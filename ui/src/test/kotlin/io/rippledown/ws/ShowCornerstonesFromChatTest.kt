package io.rippledown.ws

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.chat.clickChatIconToggle
import io.rippledown.chat.requireChatPanelIsDisplayed
import io.rippledown.cornerstone.requireCornerstoneCase
import io.rippledown.cornerstone.requireIndexAndTotalToBeDisplayed
import io.rippledown.cornerstone.requireNoCornerstoneCaseToBeShowing
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.OpenRDRUI
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class ShowCornerstonesFromChatTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: Handler
    lateinit var api: Api

    @Before
    fun setUp() {
        api = mockk<Api>(relaxed = true)
        handler = mockk<Handler>(relaxed = true)
        coEvery { api.cornerstoneStatus() } returns null
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    fun `should show Cornerstones if a chat rule session is in progress`() = runTest {
        //Given
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case
        val expectedStatus = CornerstoneStatus(
            cornerstoneToReview = createViewableCase(caseB, 2),
            indexOfCornerstoneToReview = 1,
            numberOfCornerstones = 3
        )
        lateinit var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)

        coEvery {
            api.startWebSocketSession(
                updateCornerstoneStatus = any(),
                ruleSessionCompleted = any()
            )
        } coAnswers {
            updateCornerstoneStatus = firstArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseA)
            clickChatIconToggle()
            requireChatPanelIsDisplayed()

            //When
            updateCornerstoneStatus.invoke(expectedStatus)

            //Then
            requireIndexAndTotalToBeDisplayed(1, 3)
            requireCornerstoneCase(caseB)
        }
    }

    @Test
    fun `should hide the Cornerstone view after a chat rule session has completed`() = runTest {
        //Given
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseIds = listOf(caseId1)
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(caseA, 1, listOf(bondiComment))
        coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
        coEvery { api.getCase(1) } returns case
        val expectedStatus = CornerstoneStatus(
            cornerstoneToReview = createViewableCase(caseB, 2),
            indexOfCornerstoneToReview = 1,
            numberOfCornerstones = 3
        )
        lateinit var updateCornerstoneStatus: ((CornerstoneStatus) -> Unit)
        lateinit var ruleCompleted: (() -> Unit)

        coEvery {
            api.startWebSocketSession(
                updateCornerstoneStatus = any(),
                ruleSessionCompleted = any()
            )
        } coAnswers {
            updateCornerstoneStatus = firstArg()
            ruleCompleted = secondArg()
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Dispatchers.Unconfined)
            }
            waitForCaseToBeShowing(caseA)
            clickChatIconToggle()
            requireChatPanelIsDisplayed()

            updateCornerstoneStatus.invoke(expectedStatus)
            requireIndexAndTotalToBeDisplayed(1, 3)
            requireCornerstoneCase(caseB)

            //When
            ruleCompleted.invoke()

            //Then
            requireNoCornerstoneCaseToBeShowing()
        }
    }
}