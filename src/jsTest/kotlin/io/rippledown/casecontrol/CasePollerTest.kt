package io.rippledown.casecontrol

import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.main.Api
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.TestResult
import mocks.config
import mocks.mock
import proxy.requireNumberOfCases
import proxy.requireNumberOfCasesNotToBeShowing
import proxy.waitForEvents
import proxy.waitForNextPoll
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CasePollerTest {

    @Test
    fun shouldGetNumberOfCases(): TestResult {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId(1, "case 1"),
                    CaseId(2, "case 2"),
                    CaseId(3, "case 3")
                )
            )
            returnCase = createCase("case 1", 1)
        }

        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireNumberOfCases(3)
            }
        }
    }

    @Test
    fun shouldNotShowCaseViewWhenThereAreNoMoreCases(): TestResult {
        val config = config {
            val caseId1 = CaseId(1, "case 1")
            val caseId2 = CaseId(2, "case 2")
            val caseId3 = CaseId(3, "case 3")
            returnCasesInfo = CasesInfo(
                listOf(
                    caseId1,
                    caseId2,
                    caseId3
                )
            )
            returnCase = createCase(caseId1)
        }

        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireNumberOfCases(3)
                config.returnCasesInfo = CasesInfo(emptyList())
                waitForNextPoll()
                requireCaseSelectorNotToBeShowing()
            }
        }
    }

    @Test
    fun shouldShowNamesOnTheCaseList(): TestResult {
        val case1 = "case 1"
        val case2 = "case 2"
        val caseId1 = CaseId(1, case1)
        val caseId2 = CaseId(2, case2)
        val caseIds = listOf(caseId1, caseId2)
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
            returnCase = createCase(caseId1)
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireNumberOfCases(2)
                requireNamesToBeShowingOnCaseList(case1, case2)
                requireCaseToBeShowing(case1)
            }
        }
    }

    @Test
    fun shouldShowCaseViewWhenACaseIsSelected(): TestResult {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseId1 = CaseId(1, caseName1)
        val caseId2 = CaseId(2, caseName2)
        val caseIds = listOf(
            caseId1,
            caseId2,
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseId1)
        }

        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireNamesToBeShowingOnCaseList(caseName1, caseName2)
                requireCaseToBeShowing(caseName1)

                config.returnCase = createCase(caseId2)
                selectCaseByName(caseName2)
                waitForEvents()
                requireCaseToBeShowing(caseName2)
            }
        }
    }

    @Test
    fun shouldShowNoCasesWhenThereAreNoMoreCases(): TestResult {
        val caseName = "case 1"
        val caseId = CaseId(1, caseName)
        val caseIds = listOf(caseId)
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseId)
        }

        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireNamesToBeShowingOnCaseList(caseName)

                config.returnCasesInfo = CasesInfo(emptyList())
                waitForNextPoll()
                requireNumberOfCasesNotToBeShowing()
            }
        }
    }


    @Test
    fun shouldSelectTheFirstCaseWhenTheSelectedCaseHasBeenDeleted(): TestResult {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseName3 = "case 3"
        val caseId1 = CaseId(1, caseName1)
        val caseId2 = CaseId(2, caseName2)
        val caseId3 = CaseId(3, caseName3)
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(caseId1, caseId2, caseId3)
            )
            returnCase = createCase(caseId2)
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                selectCaseByName(caseName2)
                requireCaseToBeShowing(caseName2)

                //set the mock to return the other two cases
                config.returnCasesInfo = CasesInfo(
                    listOf(caseId1, caseId3)
                )
                config.returnCase = createCase(caseId1)
                waitForNextPoll()
                requireCaseToBeShowing(caseName1)
                requireNamesToBeShowingOnCaseList(caseName1, caseName3)
            }
        }
    }

    @Test
    fun shouldSelectTheFirstCaseByDefault(): TestResult {
        val case1 = "case 1"
        val case2 = "case 2"
        val caseId1 = CaseId(1, case1)
        val caseId2 = CaseId(2, case2)
        val caseIds = listOf(
            caseId1,
            caseId2,
        )
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
            returnCase = createCase(caseId1)
        }
        val fc = FC {
            CasePoller {
                api = Api(mock(config))
                scope = MainScope()
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                requireCaseToBeShowing(case1)
            }
        }
    }
}
