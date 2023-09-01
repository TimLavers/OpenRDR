package io.rippledown.interpretation

import io.rippledown.casecontrol.interpretationTabsKey
import io.rippledown.model.Interpretation
import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import mui.material.Button
import proxy.findById
import react.FC
import react.dom.createRootFor
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.useState
import kotlin.test.Test

class InterpretationTabsUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenInterpretationTextIsChanged() = runTest {
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"

        val interpA = Interpretation(verifiedText = caseAConclusion)
        val interpB = Interpretation(verifiedText = caseBConclusion)

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
        with(createRootFor(fc)) {
            //Given
            requireInterpretation(caseAConclusion)

            //When switch cases
            act { findById(buttonId).click() }

            //Then
            requireInterpretation(caseBConclusion)
        }
    }

    @Test
    fun shouldUpdateInterpretationWhenDiffListIsChanged() = runTest {

        val diffListA = DiffList(listOf(Addition(), Removal()))
        val diffListB = DiffList(listOf(Unchanged(), Replacement(), Unchanged(), Addition()))

        val comment = "Go to Bondi now!"
        val interpA = Interpretation(verifiedText = comment, diffList = diffListA)
        val interpB = Interpretation(verifiedText = comment, diffList = diffListB)

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
        with(createRootFor(fc)) {
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