package io.rippledown.cornerstone

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.coEvery
import io.mockk.mockk
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.OF
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CornerstonePagerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val caseNamePrefix = "Greta"
    val index = 42
    val total = 100
    val case = createCase(caseNamePrefix)
    val cornerstoneStatus = CornerstoneStatus(
        cornerstoneToReview = createCase(caseNamePrefix),
        indexOfCornerstoneToReview = index,
        numberOfCornerstones = total
    )

    lateinit var handler: CornerstonePagerHandler

    @Before
    fun setUp() {
        handler = mockk(relaxed = true)
        coEvery { handler.selectCornerstone(any()) } answers {
            val caseId = firstArg<Int>()
            createCase("$caseNamePrefix$caseId")
        }
    }

    @Test
    fun `should show the cornerstone specified by CornerstoneStatus`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //Then
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals("$caseNamePrefix$index")
        }
    }

    @Test
    fun `clicking next should show the next cornerstone`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickNext()

            //Then
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals("$caseNamePrefix${index + 1}")
        }
    }


    @Test
    fun `clicking previous should show the previous cornerstone`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickPrevious()

            //Then
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals("$caseNamePrefix${index - 1}")
        }
    }

    @Test
    fun `clicking next should update the navigation status`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }
            onNodeWithContentDescription(INDEX_AND_TOTAL_ID)
                .assertTextEquals("${index + 1} $OF $total")//Display is 1-based

            //When
            clickNext()

            //Then
            onNodeWithContentDescription(INDEX_AND_TOTAL_ID)
                .assertTextEquals("${index + 2} $OF $total")
        }
    }

    @Test
    fun `clicking previous should update the navigation status`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }
            onNodeWithContentDescription(INDEX_AND_TOTAL_ID)
                .assertTextEquals("${index + 1} $OF $total") //Display is 1-based

            //When
            clickPrevious()

            //Then
            onNodeWithContentDescription(INDEX_AND_TOTAL_ID)
                .assertTextEquals("${index} $OF $total")
        }
    }


}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {

            CornerstonePager(
                CornerstoneStatus(createCase("Greta"), 2, 5), object : CornerstonePagerHandler {
                    override suspend fun selectCornerstone(index: Int): ViewableCase {
                        return createCase("Greta ${index + 1}")
                    }
                })
        }
    }
}
