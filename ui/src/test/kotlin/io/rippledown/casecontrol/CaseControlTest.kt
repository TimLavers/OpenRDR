package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.cornerstone.*
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import io.rippledown.utils.createViewableCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseControlTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseControlHandler

    @Before
    fun setUp() {
        handler = mockk<CaseControlHandler>()
    }

    @Test
    fun `should show the interpretative report of the case`() = runTest {
        val name = "case A"
        val bondiComment = "Go to Bondi"
        val case = createViewableCaseWithInterpretation(name, 1, listOf(bondiComment))

        with(composeTestRule) {
            //Given
            setContent {
                CaseControl(
                    currentCase = case,
                    handler = handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show case view`() = runTest {
        val viewableCase = createViewableCaseWithInterpretation(
            name = "case 1",
            caseId = 1,
            conclusionTexts = listOf()
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = viewableCase,
                    handler = handler
                )
            }
            waitForCaseToBeShowing("case 1")
        }
    }

    @Test
    fun `should show cornerstone index and total when cornerstone status is provided`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone B", 2)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneCase("cornerstone B")
            requireCornerstoneLabel("$CORNERSTONE_TITLE 1 of 3")
        }
    }

    @Test
    fun `should show correct index for second cornerstone of five`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone C", 3)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 1,
            numberOfCornerstones = 5
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneLabel("$CORNERSTONE_TITLE 2 of 5")
        }
    }

    @Test
    fun `should show correct index for last cornerstone`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone D", 4)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 2,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireCornerstoneLabel("$CORNERSTONE_TITLE 3 of 3")
        }
    }

    @Test
    fun `should not show cornerstone index and total when no cornerstone status`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    handler = handler
                )
            }
            requireNoCornerstoneLabel()
        }
    }

    @Test
    fun `should show no cornerstones to review message when cornerstone status has no cornerstone to review`() =
        runTest {
            val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
            val status = CornerstoneStatus()

            with(composeTestRule) {
                setContent {
                    CaseControl(
                        currentCase = currentCase,
                        cornerstoneStatus = status,
                        handler = handler
                    )
                }
                requireNoCornerstonesToReviewMessage()
            }
        }

    @Test
    fun `should not show no cornerstones to review message when cornerstone status is null`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    handler = handler
                )
            }
            requireNoNoCornerstonesToReviewMessage()
        }
    }

    @Test
    fun `should not show no cornerstones to review message when there are cornerstones`() = runTest {
        val currentCase = createViewableCaseWithInterpretation("case A", 1, listOf())
        val cornerstoneCase = createViewableCase("cornerstone B", 2)
        val status = CornerstoneStatus(
            cornerstoneToReview = cornerstoneCase,
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        with(composeTestRule) {
            setContent {
                CaseControl(
                    currentCase = currentCase,
                    cornerstoneStatus = status,
                    handler = handler
                )
            }
            requireNoNoCornerstonesToReviewMessage()
        }
    }
}