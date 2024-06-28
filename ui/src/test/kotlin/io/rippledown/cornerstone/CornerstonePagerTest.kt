package io.rippledown.cornerstone

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
        cornerstoneToReview = case,
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
            requireCornerstoneCase("$caseNamePrefix$index")

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
    fun `clicking next should show the next cornerstone`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }

            //When
            clickNext()

            //Then
            requireCornerstoneCase("$caseNamePrefix${index + 1}")

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
            requireCornerstoneCase("$caseNamePrefix${index - 1}")
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
            coVerify { handler.exemptCornerstone(index) }
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

    @Test
    fun `clicking next should update the navigation status`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }
            requireIndexAndTotalToBeDisplayed(index, total)


            //When
            clickNext()

            //Then
            requireIndexAndTotalToBeDisplayed(index + 1, total)
        }
    }

    @Test
    fun `clicking previous should update the navigation status`() {
        with(composeTestRule) {
            //Given
            setContent {
                CornerstonePager(cornerstoneStatus, handler)
            }
            requireIndexAndTotalToBeDisplayed(index, total)

            //When
            clickPrevious()

            //Then
            requireIndexAndTotalToBeDisplayed(index - 1, total)
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

                    override fun exemptCornerstone(index: Int): CornerstoneStatus {
                        TODO("Not yet implemented")
                    }
                })
        }
    }
}
