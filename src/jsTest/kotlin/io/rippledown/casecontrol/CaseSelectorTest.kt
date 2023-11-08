package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CaseSelectorTest {

    @Test
    fun shouldListCaseNames(): TestResult {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = 1, name = caseA), CaseId(id = 2, name = caseB)
        )

        val fc = FC {
            CaseSelector {
                caseIds = twoCaseIds
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                requireNamesToBeShowingOnCaseList(caseA, caseB)
            }
        }
    }

    @Test
    fun shouldCallSelectedCaseWhenCaseIsSelectedById(): TestResult {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        var selectedCaseId: Long = 0
        val fc = FC {
            CaseSelector {
                caseIds = threeCaseIds
                selectCase = { id ->
                    selectedCaseId = id
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                selectCaseByName(caseId2.name)
                selectedCaseId shouldBe caseId2.id
            }
        }
    }

    @Test
    fun shouldSetTheInitialListSelection(): TestResult {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val fc = FC {
            CaseSelector {
                caseIds = threeCaseIds
                selectedCaseName = caseB
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireNameOnCaseListToBeSelected(caseB)
            }
        }
    }
}
