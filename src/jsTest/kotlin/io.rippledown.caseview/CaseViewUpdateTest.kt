package io.rippledown.caseview

import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.Interpretation
import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import mui.material.Button
import proxy.findById
import proxy.waitForEvents
import react.FC
import react.dom.createRootFor
import react.dom.test.act
import react.useState
import kotlin.test.Test

class CaseViewUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenCaseIsChanged() = runTest {
        val caseIdA = CaseId(id = 1, name = "case A")
        val caseIdB = CaseId(id = 2, name = "case B")
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val buttonId = "button_id"

        val caseA = createCase(caseIdA)
        val caseB = createCase(caseIdB)
        val interpA = Interpretation(verifiedText = caseAConclusion)
        val interpB = Interpretation(verifiedText = caseBConclusion)

        val fc = FC {
            var currentCase by useState(caseA)
            var currentInterp by useState(interpA)

            Button {
                id = buttonId
                onClick = {
                    currentCase = caseB
                    currentInterp = interpB
                }
            }

            CaseView {
                case = currentCase
                currentInterpretation = currentInterp

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