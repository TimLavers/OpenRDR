package io.rippledown.cornerstone

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.cornerstone.*
import io.rippledown.interpretation.requireNoDifferencesTab
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import org.junit.Rule
import org.junit.Test

class CornerstoneInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val name = "Greta"
    val case = createCase(name)
    val cornerstoneStatus = CornerstoneStatus(case, 42, 100)

    @Test
    fun `should display the 'no cornerstones to review' message when there are no cornerstones`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(CornerstoneStatus())
            }
            onNodeWithContentDescription(NO_CORNERSTONES_TO_REVIEW_ID)
                .assertTextEquals(NO_CORNERSTONES_TO_REVIEW_MSG)
        }
    }

    @Test
    fun `should display a label for the cornerstone case`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(cornerstoneStatus)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals(CORNERSTONE_TITLE)
        }
    }

    @Test
    fun `should display the name of the cornerstone case`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(cornerstoneStatus)
            }
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals(name)
        }

    }

    @Test
    fun `should not show the differences tab`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(cornerstoneStatus)
            }
            requireNoDifferencesTab()
        }

    }
}


fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val case = createCase("Greta")
            val cornerstoneStatus = CornerstoneStatus(case, 42, 100)
            CornerstoneInspection(cornerstoneStatus)
        }
    }
}