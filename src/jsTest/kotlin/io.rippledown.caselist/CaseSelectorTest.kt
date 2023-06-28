package io.rippledown.caselist

import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CaseSelectorTest {

    @Test
    fun shouldListCaseNames() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = 1, name = caseA), CaseId(id = 2, name = caseB)
        )

        val vfc = VFC {
            CaseSelector {
                caseIds = twoCaseIds
            }
        }

        checkContainer(vfc) { container ->
            with(container) {
                requireNamesToBeShowingOnCaseList(caseA, caseB)
            }
        }
    }

    @Test
    fun shouldCallSelectedCaseWhenCaseIsSelectedById() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        var selectedCaseId: Long = 0
        val vfc = VFC {
            CaseSelector {
                caseIds = threeCaseIds
                selectCase = { id ->
                    selectedCaseId = id
                }
            }
        }
        with(createRootFor(vfc)) {
            selectCaseByName(caseId2.name)
            selectedCaseId shouldBe caseId2.id
        }
    }

    @Test
    fun shouldSetTheInitialListSelection() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val vfc = VFC {
            CaseSelector {
                caseIds = threeCaseIds
                selectedCaseName = caseB
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireNameOnCaseListToBeSelected(caseB)
            }
        }
    }
}
