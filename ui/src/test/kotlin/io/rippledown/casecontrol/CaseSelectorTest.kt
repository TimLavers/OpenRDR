package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.caseview.CASELIST_ID
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
    fun `should scroll to the middle of the case names`() = runTest {
        val caseIds = (0..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler)
            }
            //Given
            composeTestRule.onNodeWithContentDescription(contentDescription(50)).assertDoesNotExist()

            //When
            composeTestRule.onNodeWithContentDescription(CASELIST_ID).performScrollToIndex(50)

            //Then
            composeTestRule.onNodeWithContentDescription(contentDescription(50)).assertIsDisplayed()
        }
    }

    @Test
    fun `should scroll to the end of the case names`() = runTest {
        val caseIds = (0..100).map { i ->
            CaseId(id = i.toLong(), name = "case $i")
        }

        with(composeTestRule) {
            setContent {
                CaseSelector(caseIds, handler)
            }
            //Given
            composeTestRule.onNodeWithContentDescription(contentDescription(100)).assertDoesNotExist()

            //When
            composeTestRule.onNodeWithContentDescription(CASELIST_ID).performScrollToIndex(100)

            //Then
            composeTestRule.onNodeWithContentDescription(contentDescription(100)).assertIsDisplayed()
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
