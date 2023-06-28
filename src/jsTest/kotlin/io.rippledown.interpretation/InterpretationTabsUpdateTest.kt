package io.rippledown.interpretation

import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import mui.material.Button
import proxy.findById
import proxy.waitForEvents
import react.VFC
import react.dom.createRootFor
import react.dom.test.act
import react.useState
import kotlin.test.Test

class InterpretationTabsUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenCaseIsChanged() = runTest {
        val caseIdA = CaseId(id = 1, name = "case A")
        val caseIdB = CaseId(id = 2, name = "case B")
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val buttonId = "button_id"

        val caseA = createCaseWithInterpretation(caseIdA.name, caseIdA.id, listOf(caseAConclusion))
        val caseB = createCaseWithInterpretation(caseIdB.name, caseIdB.id, listOf(caseBConclusion))

        val vfc = VFC {
            var currentCase by useState<ViewableCase?>(caseA)

            Button {
                id = buttonId
                onClick = {
                    currentCase = caseB
                }
            }

            InterpretationTabs {
                interpretation = currentCase!!.interpretation
            }
        }
        with(createRootFor(vfc)) {
            requireInterpretation(caseAConclusion)

            //switch cases
            act { findById(buttonId).click() }
            waitForEvents()

            requireInterpretation(caseBConclusion)
        }
    }
}