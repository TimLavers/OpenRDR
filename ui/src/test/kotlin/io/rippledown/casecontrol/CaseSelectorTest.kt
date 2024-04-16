package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.model.CaseId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class CaseSelectorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: CaseSelectorHandler

    @Before
    fun setUp() {
        handler = mockk<CaseSelectorHandler>(relaxed = true)
    }

    @Test
    fun `should list case names `() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = 1, name = caseA), CaseId(id = 2, name = caseB)
        )
        with(composeTestRule) {
            setContent {
                CaseSelector(twoCaseIds, handler)
            }
            requireNamesToBeShowingOnCaseList(caseA, caseB)
        }
    }

    @Test
    fun `should call selectCase when case is selected by name`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)

        with(composeTestRule) {
            setContent {
                CaseSelector(threeCaseIds, handler)
            }
            //Given

            //When
            selectCaseByName(caseId2.name)

            //Then
            verify { handler.selectCase(caseId2.id!!) }
        }
    }

    @Test
    fun `should be able to select the next case name using the keyboard`() = runTest {
        val case0 = "case 0"
        val case1 = "case 1"

        val caseIds = (0..1).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler)
            }
            //Given
            composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
            selectCaseByName(case0)

            //When
            downArrowOnCase(case0)

            //Then
            requireCaseToBeFocused(case1)
        }
    }

    @Test
    fun `should be able to select the previous case name using the keyboard`() {
        val case0 = "case 0"
        val case1 = "case 1"
        runTest {
            val caseIds = (0..1).map { i ->
                CaseId(id = i.toLong(), name = "case $i")
            }

            with(composeTestRule) {
                setContent {
                    CaseSelector(caseIds, handler)
                }
                //Given
                composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
                selectCaseByName(case1)

                //When
                upArrowOnCase(case1)

                //Then
                requireCaseToBeFocused(case0)
            }
        }
    }

    @Test
    fun `should not be able to down arrow past the last case`() {
        val case1 = "case 1"
        runTest {
            val caseIds = (0..1).map { i ->
                CaseId(id = i.toLong(), name = "case $i")
            }

            with(composeTestRule) {
                setContent {
                    CaseSelector(caseIds, handler)
                }
                //Given
                composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
                selectCaseByName(case1)

                //When
                downArrowOnCase(case1)

                //Then
                requireCaseToBeFocused(case1)
            }
        }
    }

    @Test
    fun `should not be able to up arrow before the first case`() {
        val case0 = "case 0"
        runTest {
            val caseIds = (0..1).map { i ->
                CaseId(id = i.toLong(), name = "case $i")
            }

            with(composeTestRule) {
                setContent {
                    CaseSelector(caseIds, handler)
                }
                //Given
                composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
                selectCaseByName(case0)

                //When
                upArrowOnCase(case0)

                //Then
                requireCaseToBeFocused(case0)
            }
        }
    }

    @Test
    fun `case names should exist even if not currently showing`() = runTest {
        val caseIds = (0..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler)
            }
            composeTestRule.onNodeWithContentDescription(contentDescription(100))
                .assertIsNotDisplayed()
                .assertExists()
        }
    }

    private fun contentDescription(i: Int) = "${CASE_NAME_PREFIX}case $i"
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
    ) {
        val caseIds = (1..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }
        CaseSelector(caseIds, object : CaseSelectorHandler {
            override var selectCase: (id: Long) -> Unit = { println("selectCaseID = $it") }
        })
    }
}
