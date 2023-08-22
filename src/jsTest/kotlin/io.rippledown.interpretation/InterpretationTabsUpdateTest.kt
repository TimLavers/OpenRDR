package io.rippledown.interpretation

import io.rippledown.model.Interpretation
import kotlinx.coroutines.test.runTest
import mui.material.Button
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.useState
import kotlin.test.Test

class InterpretationTabsUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenCaseIsChanged() = runTest {
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
                key = interp.latestText() //Force re-render when the current text changes
                InterpretationTabs {
                    interpretation = interp
                }
            }
        }
        with(createRootFor(fc)) {
            requireInterpretation(caseAConclusion)

            //switch cases
            act { findById(buttonId).click() }
            waitForEvents()

            requireInterpretation(caseBConclusion)
        }
    }
}