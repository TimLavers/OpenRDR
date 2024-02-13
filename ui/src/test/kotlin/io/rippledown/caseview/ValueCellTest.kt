package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.model.TestResult
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ValueCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `show result that does not have units`() {
        val testResult = TestResult("12.8", null, null)
        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.ValueCell(testResult, 0.1F)
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
            rowScope.ValueCell(testResult, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("12.8 waves / sec"))
        }
    }
}
class DummyModifier: Modifier {
    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean {
        return true
    }

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean {
        return true
    }

    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R {
        return initial
    }

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R {
        return initial
    }
}
class DummyRowScope: RowScope {
    private val modifier = DummyModifier()
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier {
        return modifier
    }

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier {
        return modifier
    }

    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier {
        return modifier
    }

    override fun Modifier.alignByBaseline(): Modifier {
        return modifier
    }

    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier {
        return modifier
    }
}
