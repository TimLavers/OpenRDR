package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.rippledown.interpretation.requireInterpretation
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
}