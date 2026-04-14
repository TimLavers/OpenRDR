package io.rippledown.casecontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    @Test
    fun `should navigate to the next cornerstone case using the down arrow key`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(
            CaseId(id = 2, name = "c1", type = CaseType.Cornerstone),
            CaseId(id = 3, name = "c2", type = CaseType.Cornerstone)
        )
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("c1")

            //When
            downArrowOnCase("c1")

            //Then
            requireCaseToBeFocused("c2")
            verify { handler.selectCase(3) }
        }
    }

    @Test
    fun `should navigate to the previous cornerstone case using the up arrow key`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(
            CaseId(id = 2, name = "c1", type = CaseType.Cornerstone),
            CaseId(id = 3, name = "c2", type = CaseType.Cornerstone)
        )
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("c2")

            //When
            upArrowOnCase("c2")

            //Then
            requireCaseToBeFocused("c1")
            verify { handler.selectCase(2) }
        }
    }

    @Test
    fun `should navigate from last processed case to first cornerstone case using down arrow`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"), CaseId(id = 2, name = "p2"))
        val cornerstones = listOf(CaseId(id = 3, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("p2")

            //When
            downArrowOnCase("p2")

            //Then
            requireCaseToBeFocused("c1")
            verify { handler.selectCase(3) }
        }
    }

    @Test
    fun `should navigate from first cornerstone case to last processed case using up arrow`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"), CaseId(id = 2, name = "p2"))
        val cornerstones = listOf(CaseId(id = 3, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("c1")

            //When
            upArrowOnCase("c1")

            //Then
            requireCaseToBeFocused("p2")
            verify { handler.selectCase(2) }
        }
    }

    @Test
    fun `should not be able to down arrow past the last cornerstone case`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(CaseId(id = 2, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("c1")

            //When
            downArrowOnCase("c1")

            //Then
            requireCaseToBeFocused("c1")
        }
    }

    @Test
    fun `should navigate through multiple cases using successive down arrows`() = runTest {
        val caseIds = (0..2).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }
        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            //Given
            selectCaseByName("case 0")

            //When
            downArrowOnCase("case 0")
            downArrowOnCase("case 1")

            //Then
            requireCaseToBeFocused("case 2")
            verify { handler.selectCase(2) }
        }
    }

    @Test
    fun `should navigate through multiple cases using successive up arrows`() = runTest {
        val caseIds = (0..2).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }
        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            //Given
            selectCaseByName("case 2")

            //When
            upArrowOnCase("case 2")
            upArrowOnCase("case 1")

            //Then
            requireCaseToBeFocused("case 0")
            verify { handler.selectCase(0) }
        }
    }

    @Test
    fun `should navigate through multiple cornerstone cases using successive down arrows`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = (2..4).map { i ->
            CaseId(id = i.toLong(), name = "c$i", type = CaseType.Cornerstone)
        }
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("c2")

            //When
            downArrowOnCase("c2")
            downArrowOnCase("c3")

            //Then
            requireCaseToBeFocused("c4")
            verify { handler.selectCase(4) }
        }
    }

    @Test
    fun `should navigate across processed and cornerstone sections using down then up arrows`() = runTest {
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cornerstones = listOf(CaseId(id = 2, name = "c1", type = CaseType.Cornerstone))
        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            //Given
            selectCaseByName("p1")

            //When - navigate down into cornerstones, then back up
            downArrowOnCase("p1")
            upArrowOnCase("c1")

            //Then
            requireCaseToBeFocused("p1")
            verify { handler.selectCase(1) }
        }
    }

    @Test
    fun `should preserve processed case focus after recomposition with equivalent case data`() = runTest {
        //Given
        val case1 = CaseId(id = 1, name = "case 1")
        val case2 = CaseId(id = 2, name = "case 2")
        var caseIds by mutableStateOf(listOf(case1, case2))

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler = handler)
            }
            selectCaseByName("case 1")
            requireCaseToBeFocused("case 1")

            //When - trigger recomposition with new list reference but same content
            caseIds = listOf(case1.copy(), case2.copy())
            waitForIdle()

            //Then - focus should be preserved and navigation should still work
            downArrowOnCase("case 1")
            requireCaseToBeFocused("case 2")
            verify { handler.selectCase(2) }
        }
    }

    @Test
    fun `should preserve cornerstone case focus after recomposition with equivalent case data`() = runTest {
        //Given
        val processed = listOf(CaseId(id = 1, name = "p1"))
        val cs1 = CaseId(id = 2, name = "c1", type = CaseType.Cornerstone)
        val cs2 = CaseId(id = 3, name = "c2", type = CaseType.Cornerstone)
        var cornerstones by mutableStateOf(listOf(cs1, cs2))

        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            selectCaseByName("c1")
            requireCaseToBeFocused("c1")

            //When - trigger recomposition with new list reference but same content
            cornerstones = listOf(cs1.copy(), cs2.copy())
            waitForIdle()

            //Then - focus should be preserved and navigation should still work
            downArrowOnCase("c1")
            requireCaseToBeFocused("c2")
            verify { handler.selectCase(3) }
        }
    }

    @Test
    fun `should preserve cross-section focus after recomposition with equivalent case data`() = runTest {
        //Given
        val p1 = CaseId(id = 1, name = "p1")
        val c1 = CaseId(id = 2, name = "c1", type = CaseType.Cornerstone)
        var processed by mutableStateOf(listOf(p1))
        var cornerstones by mutableStateOf(listOf(c1))

        with(composeTestRule) {
            setContent {
                CaseSelector(processed, cornerstones, handler)
            }
            selectCaseByName("p1")
            requireCaseToBeFocused("p1")

            //When - trigger recomposition with new references
            processed = listOf(p1.copy())
            cornerstones = listOf(c1.copy())
            waitForIdle()

            //Then - cross-section navigation should still work
            downArrowOnCase("p1")
            requireCaseToBeFocused("c1")
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
            override var requestFocusOnSelectedCase: () -> Unit = {}
        })
    }
}
