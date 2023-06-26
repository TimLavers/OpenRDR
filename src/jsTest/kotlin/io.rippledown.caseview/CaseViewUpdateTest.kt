package io.rippledown.caseview

import Api
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import mui.material.Button
import proxy.findById
import proxy.waitForEvents
import react.VFC
import react.dom.createRootFor
import react.dom.test.act
import react.useState
import kotlin.test.Test

class CaseViewUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenCaseIsChanged() = runTest {
        val caseIdA = CaseId(id = "1", name = "case A")
        val caseIdB = CaseId(id = "2", name = "case B")
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val buttonId = "button_id"

        val caseA = createCaseWithInterpretation(caseIdA.id, caseIdA.name, listOf(caseAConclusion))
        val caseB = createCaseWithInterpretation(caseIdB.id, caseIdB.name, listOf(caseBConclusion))

        val config = config {
            returnCase = caseA
        }
        val vfc = VFC {
            var currentCase by useState<ViewableCase?>(caseA)

            Button {
                id = buttonId
                onClick = {
                    currentCase = caseB
                }
            }

            CaseView {
                case = currentCase!!
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            requireInterpretation(caseAConclusion)
            config.returnCase = caseB

            //switch cases
            act { findById(buttonId).click() }
            waitForEvents()

            requireInterpretation(caseBConclusion)
        }
    }
}