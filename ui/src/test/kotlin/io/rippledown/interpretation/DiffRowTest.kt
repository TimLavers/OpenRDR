package io.rippledown.interpretation

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.rippledown.constants.interpretation.DIFF_ROW_ADDITION
import io.rippledown.constants.interpretation.DIFF_ROW_REMOVAL
import io.rippledown.constants.interpretation.DIFF_ROW_REPLACEMENT_NEW
import io.rippledown.constants.interpretation.DIFF_ROW_REPLACEMENT_ORIGINAL
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class DiffRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show addition with green background`() = runTest {
        with(composeTestRule) {
            setContent {
                DiffRow(Addition("Go to Bondi."))
            }
            onNodeWithContentDescription(DIFF_ROW_ADDITION).assertTextEquals("Go to Bondi.")
        }
    }

    @Test
    fun `should show removal with red background`() = runTest {
        with(composeTestRule) {
            setContent {
                DiffRow(Removal("Go to Bondi."))
            }
            onNodeWithContentDescription(DIFF_ROW_REMOVAL).assertTextEquals("Go to Bondi.")
        }
    }

    @Test
    fun `should show replacement with original in red and new in green`() = runTest {
        with(composeTestRule) {
            setContent {
                DiffRow(Replacement("Go to Bondi.", "Go to Maroubra."))
            }
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_ORIGINAL).assertTextEquals("Go to Bondi.")
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_NEW).assertTextEquals("Go to Maroubra.")
        }
    }

    @Test
    fun `should not show removal node for an addition`() = runTest {
        with(composeTestRule) {
            setContent {
                DiffRow(Addition("Go to Bondi."))
            }
            onNodeWithContentDescription(DIFF_ROW_REMOVAL).assertDoesNotExist()
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_ORIGINAL).assertDoesNotExist()
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_NEW).assertDoesNotExist()
        }
    }

    @Test
    fun `should not show addition node for a removal`() = runTest {
        with(composeTestRule) {
            setContent {
                DiffRow(Removal("Go to Bondi."))
            }
            onNodeWithContentDescription(DIFF_ROW_ADDITION).assertDoesNotExist()
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_ORIGINAL).assertDoesNotExist()
            onNodeWithContentDescription(DIFF_ROW_REPLACEMENT_NEW).assertDoesNotExist()
        }
    }
}
