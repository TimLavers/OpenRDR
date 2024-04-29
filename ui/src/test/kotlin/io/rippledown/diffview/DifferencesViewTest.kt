package io.rippledown.diffview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.interpretation.DifferencesView
import io.rippledown.interpretation.DifferencesViewHandler
import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DifferencesViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: DifferencesViewHandler

    @Before
    fun setUp() {
        handler = mockk<DifferencesViewHandler>(relaxed = true)
    }


    @Test
    fun `should not show any rows if no comments`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(DiffList(), handler)
            }

            //Then
            requireNumberOfRows(0)
        }
    }

    @Test
    fun `should show a row for each difflist element`() = runTest {
        val diffs = listOf(
            Unchanged("Weather is fine."),
            Addition("Go to Bondi Beach."),
            Addition("And bring flippers."),
            Replacement("Surf is good.", "Surf is great."),
            Removal("Don't stay long.")
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }
            //Then
            requireNumberOfRows(diffs.size)
        }
    }

    @Test
    fun `should show a a build icon for the first changed diff`() = runTest {
        val diffs = listOf(
            Unchanged(),
            Unchanged(),
            Addition(),
            Addition(),
            Replacement(),
            Removal()
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }
            //Then
            requireBuildIconForRow(2)
        }
    }


    @Test
    fun `should show a build icon when mouse is over a changed diff`() = runTest {
        val diffs = listOf(
            Unchanged(),
            Unchanged(),
            Addition(),
            Addition(),
            Replacement(),
            Removal()
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }
            //When
            moveMouseOverRow(2)

            //Then
            requireBuildIconForRow(2)
        }
    }

    @Test
    fun `should not show a build icon when mouse is over an unchanged diff`() = runTest {
        val diffs = listOf(
            Unchanged(),
            Unchanged(),
            Addition(),
            Addition(),
            Replacement(),
            Removal()
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }
            //When
            moveMouseOverRow(1)

            //Then
            requireNoBuildIconForRow(1)
        }
    }


    @Test
    fun `should show unchanged text in both the original and unchanged columns`() = runTest {
        val text = "Go to Bondi now!"
        val diffs = listOf(
            Unchanged(text),
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }

            //Then
            requireOriginalTextInRow(0, text)
            requireChangedTextInRow(0, text)
        }
    }

    @Test
    fun `should show an Addition in the changed column only`() = runTest {
        val text = "Go to Bondi now!"
        val diffs = listOf(
            Addition(text),
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }

            //Then
            requireOriginalTextInRow(0, "")
            requireChangedTextInRow(0, text)
        }
    }

    @Test
    fun `should show a Removal in the original column only`() = runTest {
        val text = "Go to Bondi now!"
        val diffs = listOf(
            Removal(text),
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }

            //Then
            requireOriginalTextInRow(0, text)
            requireChangedTextInRow(0, "")
        }
    }

    @Test
    fun `should show a replacement diff using both columns`() = runTest {
        val original = "Stay at home."
        val replacement = "Go to Bondi now!"
        val diffs = listOf(
            Replacement(original, replacement),
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }

            //Then
            requireOriginalTextInRow(0, original)
            requireChangedTextInRow(0, replacement)
        }
    }

    @Test
    fun `should call handler when the build icon is clicked`() = runTest {
        val original = "Stay at home."
        val replacement = "Go to Bondi now!"
        val addition = "And bring flippers."
        val removal = "Don't stay long."
        val diffs = listOf(
            Addition(addition),
            Replacement(original, replacement),
            Removal(removal)
        )
        val diffList = DiffList(diffs)

        with(composeTestRule) {
            //Given
            setContent {
                DifferencesView(diffList, handler)
            }

            //When
            clickBuildIconForRow(1)

            //Then
            verify { handler.onStartRule(diffs[1]) }
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val handler = mockk<DifferencesViewHandler>(relaxed = true)
            val diffList = DiffList(
                listOf(
                    Unchanged("Weather is fine."),
                    Addition("Go to Bondi Beach."),
                    Addition("And bring flippers."),
                    Replacement("Surf is good.", "Surf is great."),
                    Removal("Don't stay long.")
                )
            )
            DifferencesView(diffList, handler)
        }
    }
}
