package io.rippledown.interpretation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.em
import io.kotest.matchers.shouldBe
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
    fun `shows formula even if there are no conditions`() = runTest {
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

    @Test
    fun `formula exponent is rendered as a superscript`() = runTest {
        val info = DerivedValueInfo(
            name = "BMI",
            value = "25.3",
            formula = "Weight / Height ** 2",
            conditions = listOf("Height is high")
        )

        with(composeTestRule) {
            setContent { DerivedValueTooltip(info) }

            val node = onNodeWithContentDescription("$DERIVED_VALUE_FORMULA_PREFIX${info.formula}")
                .fetchSemanticsNode()
            val annotated = node.config.getOrNull(SemanticsProperties.Text)!!.first()

            annotated.text shouldBe "Weight / Height 2"
            val superscript = annotated.spanStyles.first { it.item.baselineShift == BaselineShift.Superscript }
            annotated.text.substring(superscript.start, superscript.end) shouldBe "2"
            superscript.item.fontSize shouldBe 0.85.em
            superscript.item.fontStyle shouldBe FontStyle.Italic
        }
    }

    @Test
    fun `formula text is italic`() = runTest {
        val info = DerivedValueInfo(
            name = "BMI",
            value = "25.3",
            formula = "Weight / (Height * Height)",
            conditions = listOf("Weight is high")
        )

        with(composeTestRule) {
            setContent { DerivedValueTooltip(info) }

            val node = onNodeWithContentDescription("$DERIVED_VALUE_FORMULA_PREFIX${info.formula}")
                .fetchSemanticsNode()
            val annotated = node.config.getOrNull(SemanticsProperties.Text)!!.first()

            annotated.text shouldBe info.formula
            annotated.spanStyles.size shouldBe 1
            val span = annotated.spanStyles[0]
            span.start shouldBe 0
            span.end shouldBe info.formula.length
            span.item.fontStyle shouldBe FontStyle.Italic
        }
    }
}
