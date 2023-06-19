package io.rippledown.caselist

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.coroutines.test.runTest
import proxy.*
import react.VFC
import react.dom.checkContainer
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
    fun shouldCallSelectedCaseWhenCaseNameIsSelected() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        lateinit var selectedCase: String
        val vfc = VFC {
            CaseSelector {
                caseIds = threeCaseIds
                selectCase = { caseName ->
                    selectedCase = caseName
                }
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                selectCase(caseB)
                selectedCase shouldBe caseB
            }
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
