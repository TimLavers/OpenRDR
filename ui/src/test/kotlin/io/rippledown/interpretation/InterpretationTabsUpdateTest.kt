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
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.Diff
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class InterpretationTabsUpdateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val textA = "text for case A"
    val textB = "text for case B"
    val i1 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(1, textA))) }
    val i2 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(2, textB))) }
    val interpA = ViewableInterpretation(i1)
    val interpB = ViewableInterpretation(i2)


    @Test
    fun `should update interpretation when the interpretation text is changed`() = runTest {

        with(composeTestRule) {
            setContent {
                InterpretationTabsWithButton()
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
    fun InterpretationTabsWithButton() {
        var viewableInterpretation : ViewableInterpretation by remember { mutableStateOf(interpA) }

        key(viewableInterpretation.latestText()) {
            InterpretationTabs(object : InterpretationTabsHandler {
                override var interpretation = viewableInterpretation
                override var onStartRule: (selectedDiff: Diff) -> Unit = { }
                override var isCornerstone = false
            })
        }

        Button(onClick = {
            viewableInterpretation = interpB
        },
            modifier = Modifier.testTag("buttonTag")
        ) {
            //Button
        }
    }



/*
    @Test
    fun shouldUpdateInterpretationWhenWhenVerifiedTextIsChanged(): TestResult {
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"

        val interpA = ViewableInterpretation().apply { verifiedText = caseAConclusion }
        val interpB = ViewableInterpretation().apply { verifiedText = caseBConclusion }

        val buttonId = "button_id"

        val fc = FC {
            var interp by useState(interpA)

            Button {
                id = buttonId
                onClick = {
                    interp = interpB
                }
            }
            div {
                key = interpretationTabsKey(interp) //Re-render when the interpretation changes
                InterpretationTabs {
                    interpretation = interp
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireInterpretation(caseAConclusion)

                //When switch cases
                act { findById(buttonId).click() }

                //Then
                requireInterpretation(caseBConclusion)
            }
        }
    }
*/

}