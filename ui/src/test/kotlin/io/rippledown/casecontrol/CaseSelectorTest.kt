package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.CORNERSTONE_SECTION_HEADER_ID
import io.rippledown.constants.caseview.PROCESSED_SECTION_HEADER_ID
import io.rippledown.model.CaseId
import io.rippledown.model.CaseType
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
        handler = mockk<CaseSelectorHandler>()
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
                CaseSelector(twoCaseIds, handler = handler)
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
                CaseSelector(threeCaseIds, handler = handler)
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
                CaseSelector(caseIds, handler = handler)
            }
            //Given
            composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
            selectCaseByName(case0)

            //When
            downArrowOnCase(case0)

            //Then
            requireCaseToBeFocused(case1)

            verify { handler.selectCase(caseIds[1].id!!) }
        }
    }

    @Test
    fun `should be able to select the previous case name using the keyboard`() = runTest {
        val case0 = "case 0"
        val case1 = "case 1"
        val caseIds = (0..1).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            //Given
            composeTestRule.onNodeWithContentDescription(contentDescription(0)).assertIsDisplayed()
            selectCaseByName(case1)

            //When
            upArrowOnCase(case1)

            //Then
            requireCaseToBeFocused(case0)

            verify { handler.selectCase(caseIds[0].id!!) }
        }
    }

    @Test
    fun `should not be able to down arrow past the last case`() = runTest {
        val case1 = "case 1"
            val caseIds = (0..1).map { i ->
                CaseId(id = i.toLong(), name = "case $i")
            }

            with(composeTestRule) {
                setContent {
                    CaseSelector(caseIds, handler = handler)
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

    @Test
    fun `should not be able to up arrow before the first case`() = runTest {
        val case0 = "case 0"
            val caseIds = (0..1).map { i ->
                CaseId(id = i.toLong(), name = "case $i")
            }

            with(composeTestRule) {
                setContent {
                    CaseSelector(caseIds, handler = handler)
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

    @Test
    fun `case names should exist even if not currently showing`() = runTest {
        val caseIds = (0..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            composeTestRule.onNodeWithContentDescription(contentDescription(100))
                .assertIsNotDisplayed()
                .assertExists()
        }
    }

    @Test
    fun `should show processed section header when processed cases exist`() = runTest {
        val caseIds = listOf(CaseId(id = 1, name = "case a"), CaseId(id = 2, name = "case b"))
        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            onNodeWithContentDescription(PROCESSED_SECTION_HEADER_ID).assertIsDisplayed()
        }
    }

    @Test
    fun `should show cornerstone section header when cornerstone cases exist`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(CaseId(id = 2, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            onNodeWithContentDescription(CORNERSTONE_SECTION_HEADER_ID).assertIsDisplayed()
        }
    }

    @Test
    fun `should show cornerstone section header even when no cornerstone cases`() = runTest {
        val caseIds = listOf(CaseId(id = 1, name = "case a"))
        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            onNodeWithContentDescription(CORNERSTONE_SECTION_HEADER_ID).assertIsDisplayed()
        }
    }

    @Test
    fun `should show both processed and cornerstone cases`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"), CaseId(id = 2, name = "p2"))
        val cornerstones = listOf(CaseId(id = 3, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            requireNamesToBeShowingOnCaseList("p1", "p2", "c1")
        }
    }

    @Test
    fun `should hide processed cases when section is collapsed`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, handler = handler)
            }
            requireNamesToBeShowingOnCaseList("p1")
            onNodeWithContentDescription(PROCESSED_SECTION_HEADER_ID).performClick()
            waitForIdle()
            onNode(caseMatcher("p1")).assertDoesNotExist()
        }
    }

    @Test
    fun `should select cornerstone case`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(CaseId(id = 2, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            selectCaseByName("c1")
            verify { handler.selectCase(2) }
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
        CaseSelector(caseIds, handler = object : CaseSelectorHandler {
            override var selectCase: (id: Long) -> Unit = { println("selectCaseID = $it") }
        })
    }
}
