package io.rippledown.casecontrol

import Api
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCaseWithInterpretation
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import react.FC
import react.dom.createRootFor
import kotlin.test.Test

class CaseControlUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenAnotherCaseNameIsClicked() = runTest {
        val caseIdA = CaseId(id = 1, name = "case A")
        val caseIdB = CaseId(id = 2, name = "case B")
        val caseAConclusion = "text for case A"
        val caseBConclusion = "text for case B"
        val twoCaseIds = listOf(caseIdA, caseIdB)
        val caseA = createCaseWithInterpretation(caseIdA.name, caseIdA.id, listOf(caseAConclusion))
        val caseB = createCaseWithInterpretation(caseIdB.name, caseIdB.id, listOf(caseBConclusion))
        val config = config {
            returnCasesInfo = CasesInfo(twoCaseIds)
            returnCase = caseA
        }
        val vfc = FC {
            CaseControl {
                caseIds = twoCaseIds
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            //Given
            requireInterpretation(caseAConclusion)

            //When
            config.returnCase = caseB
            selectCaseByName(caseIdB.name)

            //Then
            requireInterpretation(caseBConclusion)
        }
    }

}