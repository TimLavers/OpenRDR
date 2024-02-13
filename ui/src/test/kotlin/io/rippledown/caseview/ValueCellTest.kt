package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.mocks.DummyRowScope
import io.rippledown.model.Attribute
import io.rippledown.model.TestResult
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ValueCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val tsh = Attribute(12, "TSH")

    @Test
    fun `show result that does not have units`() {
        val testResult = TestResult("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell(tsh, 1, testResult, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8"))
        }
    }

    @Test
    fun `show result that has units`() {
        val testResult = TestResult("12.8", null, "waves / sec")
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell(tsh, 1, testResult, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8 waves / sec"))
        }
    }
}

