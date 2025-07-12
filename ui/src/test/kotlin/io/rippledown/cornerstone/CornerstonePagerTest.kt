package io.rippledown.cornerstone

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CornerstonePagerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val caseNamePrefix = "Greta"
    val index = 42
    val total = 100
    val case = createViewableCase(caseNamePrefix)
    val cornerstoneStatus = CornerstoneStatus(
        cornerstoneToReview = case,
        indexOfCornerstoneToReview = index,
        numberOfCornerstones = total
    )

    lateinit var handler: CornerstonePagerHandler

    @Before
    fun setUp() {
        handler = mockk()
        coEvery { handler.selectCornerstone(any()) } answers {
            val caseId = firstArg<Int>()
            createViewableCase("$caseNamePrefix$caseId")
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
            requireCornerstoneCase("${cornerstoneStatus.cornerstoneToReview?.name}")

        }
    }

    @Test
    fun `should not attempt to select a cornerstone if the index is -1`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(CornerstoneStatus(), handler)
            }
            waitForIdle()

            //Then
            coVerify(exactly = 0) { handler.selectCornerstone(-1) }
        }
    }

    @Test
    fun `clicking next should show call the handler with the index of the next cornerstone`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickNext()

            //Then
            verify { handler.selectCornerstone(index + 1) }
        }
    }


    @Test
    fun `clicking previous should show call the handler with the index of the previous cornerstone`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickPrevious()

            //Then
            verify { handler.selectCornerstone(index - 1) }
        }
    }

    @Test
    fun `clicking exempt should call the handler`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickExempt()

            //Then
            verify { handler.exemptCornerstone(index) }
        }
    }

    @Test
    fun `should show index of current cornerstone and the number of cornerstones`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }
            //Then
            requireIndexAndTotalToBeDisplayed(index, total)
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {

            CornerstonePager(
                CornerstoneStatus(createViewableCase("Greta"), 2, 5), object : CornerstonePagerHandler {
                    override fun selectCornerstone(index: Int) {
                    }

                    override fun exemptCornerstone(index: Int) {
                    }
                })
        }
    }
}
