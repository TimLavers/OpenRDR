package io.rippledown.main

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.chat.clickChatIconToggle
import io.rippledown.chat.requireChatPanelIsDisplayed
import io.rippledown.cornerstone.requireCornerstoneCase
import io.rippledown.cornerstone.requireIndexAndTotalToBeDisplayed
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.Dispatchers.Unconfined
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
        api = mockk<Api>()
        handler = mockk<Handler>()
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
        lateinit var updateCallback: ((CornerstoneStatus) -> Unit)

        coEvery {
            api.startWebSocketSession(updateCornerstoneStatus = captureLambda())
        } coAnswers {
            updateCallback = lambda<(CornerstoneStatus) -> Unit>().captured
        }

        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler, dispatcher = Unconfined)
            }
            waitForCaseToBeShowing(caseA)
            clickChatIconToggle()
            requireChatPanelIsDisplayed()

            //When
            updateCallback.invoke(expectedStatus)

            //Then
            requireIndexAndTotalToBeDisplayed(1, 3)
            requireCornerstoneCase(caseB)
        }
    }
}

