package io.rippledown.interpretation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import io.rippledown.constants.interpretation.DERIVED_VALUE_CONDITIONS_PREFIX
import io.rippledown.constants.interpretation.DERIVED_VALUE_FORMULA_PREFIX
import io.rippledown.model.caseview.DerivedValueInfo
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class DerivedValueTooltipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows formula and conditions for a non-literal formula`() = runTest {
        val info = DerivedValueInfo(
            name = "BMI",
            value = "25.3",
            formula = "Weight / (Height * Height)",
            conditions = listOf("Weight is high", "Height is high")
        )

        with(composeTestRule) {
            setContent { DerivedValueTooltip(info) }

            onNodeWithContentDescription("$DERIVED_VALUE_FORMULA_PREFIX${info.formula}")
                .assertIsDisplayed()
            info.conditions.forEach { condition ->
                onNodeWithContentDescription("$DERIVED_VALUE_CONDITIONS_PREFIX$condition")
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun `hides formula when it is a literal value repeated in the tooltip`() = runTest {
        val info = DerivedValueInfo(
            name = "Status",
            value = "high",
            formula = "\"high\"",
            conditions = listOf("Glucose > 10")
        )

        with(composeTestRule) {
            setContent { DerivedValueTooltip(info) }

            onAllNodesWithContentDescription(
                label = DERIVED_VALUE_FORMULA_PREFIX,
                substring = true
            ).assertCountEquals(0)
            onNodeWithContentDescription("$DERIVED_VALUE_CONDITIONS_PREFIX${info.conditions[0]}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `shows formula only when there are no conditions`() = runTest {
        val info = DerivedValueInfo(
            name = "Ratio",
            value = "2",
            formula = "A / B",
            conditions = emptyList()
        )

        with(composeTestRule) {
            setContent { DerivedValueTooltip(info) }

            onNodeWithContentDescription("$DERIVED_VALUE_FORMULA_PREFIX${info.formula}")
                .assertIsDisplayed()
            onAllNodesWithContentDescription(
                label = DERIVED_VALUE_CONDITIONS_PREFIX,
                substring = true
            ).assertCountEquals(0)
        }
    }
}
