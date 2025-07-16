package io.rippledown.cornerstone

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.applicationFor
import io.rippledown.utils.createViewableCase
import org.junit.Rule
import org.junit.Test

class CornerstoneInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val name = "Greta"
    val case = createViewableCase(name)
    val cornerstoneStatus = CornerstoneStatus(case, 42, 100)



    @Test
    fun `should display a label for the cornerstone case`() {
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
}


fun main() {
    applicationFor {
        CornerstoneInspection(createViewableCase("Greta"))
    }
}


