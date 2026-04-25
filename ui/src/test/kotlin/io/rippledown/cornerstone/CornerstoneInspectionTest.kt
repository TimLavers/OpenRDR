package io.rippledown.cornerstone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createLargeViewableCaseWithInterpretation
import io.rippledown.utils.createViewableCase
import org.junit.Rule
import org.junit.Test

class CornerstoneInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val name = "Greta"
    val case = createViewableCase(name)

    @Test
    fun `should display the cornerstone label without index when no total`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals(CORNERSTONE_TITLE)
        }
    }

    @Test
    fun `should display the name of the cornerstone case`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case)
            }
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals(name)
        }
    }

    @Test
    fun `should display the cornerstone label with index and total`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case, index = 0, total = 3)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals("$CORNERSTONE_TITLE 1 of 3")
        }
    }

    @Test
    fun `should display the correct 1-based index for the second cornerstone`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case, index = 1, total = 5)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals("$CORNERSTONE_TITLE 2 of 5")
        }
    }

    @Test
    fun `should display the correct index for the last cornerstone`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case, index = 4, total = 5)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals("$CORNERSTONE_TITLE 5 of 5")
        }
    }

    @Test
    fun `interpretation should remain visible when the cornerstone has many attributes`() {
        val largeCase = createLargeViewableCaseWithInterpretation(
            name = "Greta",
            caseId = 2L,
            numberOfAttributes = 80,
            conclusionTexts = listOf("Go to Bondi")
        )
        with(composeTestRule) {
            setContent {
                Box(modifier = Modifier.size(width = 600.dp, height = 500.dp)) {
                    CornerstoneInspection(largeCase, index = 0, total = 1)
                }
            }
            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD_FOR_CORNERSTONE, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `should display case name before cornerstone label`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case, index = 0, total = 2)
            }
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals(name)
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals("$CORNERSTONE_TITLE 1 of 2")
        }
    }
}


fun main() {
    applicationFor {
        CornerstoneInspection(createViewableCase("Greta"))
    }
}


