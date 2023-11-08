package io.rippledown.interpretation

import io.rippledown.casecontrol.interpretationTabsKey
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.test.TestResult
import mui.material.Button
import proxy.findById
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.dom.test.runReactTest
import react.useState
import kotlin.test.Test

class InterpretationTabsUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenInterpretationTextIsChanged(): TestResult {
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val i1 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(1, caseAConclusion))) }
        val i2 = Interpretation().apply { add(RuleSummary(conclusion = Conclusion(2, caseBConclusion))) }

        val interpA = ViewableInterpretation(i1)
        val interpB = ViewableInterpretation(i2)

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

    @Test
    fun shouldUpdateInterpretationWhenDiffListIsChanged(): TestResult {

        val diffListA = DiffList(listOf(Addition(), Removal()))
        val diffListB = DiffList(listOf(Unchanged(), Replacement(), Unchanged(), Addition()))

        val interpA = ViewableInterpretation().apply { diffList = diffListA }
        val interpB = ViewableInterpretation().apply { diffList = diffListB }

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
                selectChangesTab()
                requireNumberOfRows(diffListA.diffs.size)
                requireBuildIconForRow(0) //The first unchanged diff for interpA

                //When switch cases
                act { findById(buttonId).click() }
                selectChangesTab()

                //Then
                requireNumberOfRows(diffListB.diffs.size)
                requireBuildIconForRow(1) //The first unchanged diff for interpB
            }
        }
    }
}