package io.rippledown.interpretation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.DERIVED_VALUE_CONDITIONS_PREFIX
import io.rippledown.constants.interpretation.DERIVED_VALUE_FORMULA_PREFIX
import io.rippledown.model.caseview.AiProvenance
import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.condition.ConditionText
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class AiDerivedValueTooltipTest {
    @Test
    fun `AI details form a wrapping two column card without a repeated formula or evaluation date`() = runComposeUiTest {
        // Given an evaluated AI attribute with long criteria and a rule condition
        val criteria = "True when the notes clearly affirm that the patient personally has diabetes. " +
                "Exclude family history alone, suspected diabetes and gestational diabetes alone."
        val saved = AiProvenance("clinical_notes", criteria, "Diabetic", 1_700_000_000_000, "test-model", "revision")
        val info = mutableStateOf(DerivedValueInfo(
            "hasDiabetes", "true", "AI Boolean from clinical_notes: $criteria",
            listOf(ConditionText("Age > 50", "older patient")), provenance = saved
        ))

        // When the tooltip is shown in a narrow space
        setContent { Box(Modifier.width(360.dp)) { DerivedValueTooltip(info.value) } }

        // Then headings and values align, long criteria wrap, and the redundant details are absent
        listOf("Name" to "hasDiabetes", "Refers to" to "clinical_notes", "Criteria" to criteria, "Evidence" to "Diabetic")
            .forEach { (heading, value) ->
                val label = onNodeWithText(heading).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                val content = onNodeWithText(value).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                content.left shouldBe label.right
                content.top shouldBe label.top
            }
        val criteriaBounds = onNodeWithText(criteria).fetchSemanticsNode().boundsInRoot
        criteriaBounds.height shouldBeGreaterThan onNodeWithText("Criteria").fetchSemanticsNode().boundsInRoot.height
        onAllNodesWithContentDescription(DERIVED_VALUE_FORMULA_PREFIX, substring = true).assertCountEquals(0)
        onAllNodesWithText("Evaluated", substring = true).assertCountEquals(0)
        onAllNodesWithText("2023-", substring = true).assertCountEquals(0)
        onNodeWithText("older patient").assertIsDisplayed()
        onNodeWithContentDescription("${DERIVED_VALUE_CONDITIONS_PREFIX}Age > 50").assertIsDisplayed()
        val screenshot = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
        File("build/reports/ai-derived-value-tooltip.png").apply { parentFile.mkdirs() }
            .writeBytes(checkNotNull(screenshot.encodeToData()).bytes)

        // When the evaluation has no supporting excerpt
        runOnIdle { info.value = info.value.copy(provenance = saved.copy(evidence = null)) }

        // Then the absence is explicit in the evidence cell
        onNodeWithText("No supporting excerpt returned").assertIsDisplayed()
        onNodeWithText("Diabetic").assertDoesNotExist()

        // When an ordinary calculated attribute is displayed instead
        runOnIdle { info.value = DerivedValueInfo("BMI", "25", "Weight / Height ^ 2", emptyList()) }

        // Then its existing formula presentation remains available without AI details
        onNodeWithContentDescription("${DERIVED_VALUE_FORMULA_PREFIX}Weight / Height ^ 2").assertIsDisplayed()
        onNodeWithText("Criteria").assertDoesNotExist()
    }
}
