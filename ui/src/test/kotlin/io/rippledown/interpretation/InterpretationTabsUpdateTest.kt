package io.rippledown.interpretation

import InterpretationTabs
import InterpretationTabsHandler
import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

/**
 * Tests of the key function
 */
class InterpretationTabsUpdateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val textA = "text for case A"
    val textB = "text for case B"

    @Test
    fun `should update interpretation when the interpretation text is changed`() = runTest {
        val i1 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(1, textA))) }
        val i2 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(2, textB))) }
        val original = ViewableInterpretation(i1)
        val changed = ViewableInterpretation(i2)
        with(composeTestRule) {
            setContent {
                InterpretationTabsWithButton(original, changed)
            }
            //Given
            requireInterpretation(textA)

            //When
            onNodeWithTag("buttonTag").performClick()

            //Then
            requireInterpretation(textB)
        }
    }

    @Test
    fun `should update interpretation when the verified text is changed`() = runTest {
        val original = ViewableInterpretation().apply { verifiedText = textA }
        val changed = ViewableInterpretation().apply { verifiedText = textB }
        with(composeTestRule) {
            setContent {
                InterpretationTabsWithButton(original, changed)
            }
            //Given
            requireInterpretation(textA)

            //When
            onNodeWithTag("buttonTag").performClick()

            //Then
            requireInterpretation(textB)
        }
    }

    @Composable
    fun InterpretationTabsWithButton(original: ViewableInterpretation, changed: ViewableInterpretation) {
        var viewableInterpretation: ViewableInterpretation by remember { mutableStateOf(original) }

        key(viewableInterpretation.latestText()) {
            InterpretationTabs(object : Handler by handlerImpl, InterpretationTabsHandler {
                override var interpretation = viewableInterpretation
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var isCornerstone = false
            })
        }

        Button(
            onClick = {
                viewableInterpretation = changed
            },
            modifier = Modifier.testTag("buttonTag")
        ) {
            //Button
        }
    }
}