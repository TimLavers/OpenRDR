package io.rippledown.caselist

import Api
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import mocks.config
import mocks.mock
import proxy.requireNumberOfCases
import proxy.waitForEvents
import proxy.waitForNextPoll
import react.VFC
import react.dom.createRootFor
import kotlin.test.Test

class CasePollerTest {

    @Test
    fun shouldNotShowCaseListIfThereAreNoCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(emptyList())
        }
        val vfc = VFC {
            CasePoller {
                scope = this@runTest
                api = Api(mock(config))
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNumberOfCases(0)
        }
    }

    @Test
    fun shouldGetNumberOfCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId(1, "case 1"),
                    CaseId(2, "case 2"),
                    CaseId(3, "case 3")
                )
            )
            returnCase = createCase("case 1")
        }

        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNumberOfCases(3)
        }
    }

    @Test
    fun shouldNotShowCaseViewWhenThereAreNoMoreCases() = runTest {
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId(1, "case 1"),
                    CaseId(2, "case 2"),
                    CaseId(3, "case 3")
                )
            )
            returnCase = createCase("case 1")
        }

        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }

        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNumberOfCases(3)
            config.returnCasesInfo = CasesInfo(emptyList())
            waitForNextPoll()
            requireNumberOfCases(0)
        }
    }

    @Test
    fun shouldShowNamesOnTheCaseList() = runTest {
        val case1 = "case 1"
        val case2 = "case 2"
        val caseIds = listOf(
            CaseId(1, case1),
            CaseId(2, case2),
        )
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
            returnCase = createCase(case1)
        }
        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNumberOfCases(2)
            requireNamesToBeShowingOnCaseList(case1, case2)
            requireCaseToBeShowing(case1)
        }
    }

    @Test
    fun shouldShowCaseViewWhenACaseIsSelected() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseIds = listOf(
            CaseId(1, caseName1),
            CaseId(2, caseName2),
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseName1)
        }

        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNamesToBeShowingOnCaseList(caseName1, caseName2)
            requireCaseToBeShowing(caseName1)

            config.returnCase = createCase(caseName2)
            selectCaseById(2)
            waitForEvents()
            requireCaseToBeShowing(caseName2)
        }
    }

    @Test
    fun shouldShowNoCasesWhenThereAreNoMoreCases() = runTest {
        val caseName = "case 1"
        val caseIds = listOf(
            CaseId(1, caseName)
        )
        val config = config {
            returnCasesInfo = CasesInfo(caseIds)
            returnCase = createCase(caseName)
        }

        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireNamesToBeShowingOnCaseList(caseName)

            config.returnCasesInfo = CasesInfo(emptyList())
            waitForNextPoll()
            requireNumberOfCases(0)
        }
    }


    @Test
    fun shouldSelectTheFirstCaseWhenTheSelectedCaseHasBeenDeleted() = runTest {
        val caseName1 = "case 1"
        val caseName2 = "case 2"
        val caseName3 = "case 3"
        val config = config {
            returnCasesInfo = CasesInfo(
                listOf(
                    CaseId(1, caseName1),
                    CaseId(2, caseName2),
                    CaseId(3, caseName3),
                )
            )
            returnCase = createCase(caseName2)
        }
        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            selectCaseById(2)
            requireCaseToBeShowing(caseName2)

            //set the mock to return the other two cases
            config.returnCasesInfo = CasesInfo(
                listOf(
                    CaseId(1, caseName1),
                    CaseId(3, caseName3)
                )
            )
            config.returnCase = createCase(caseName1)
            waitForNextPoll()
            requireCaseToBeShowing(caseName1)
            requireNamesToBeShowingOnCaseList(caseName1, caseName3)
        }
    }

    @Test
    fun shouldSelectTheFirstCaseByDefault() = runTest {
        val case1 = "case 1"
        val case2 = "case 2"
        val caseIds = listOf(
            CaseId(1, case1),
            CaseId(2, case2),
        )
        val config = config {
            returnCasesInfo = CasesInfo(
                caseIds
            )
            returnCase = createCase(case1)
        }
        val vfc = VFC {
            CasePoller {
                api = Api(mock(config))
                scope = this@runTest
            }
        }
        with(createRootFor(vfc)) {
            waitForNextPoll()
            requireCaseToBeShowing(case1)
        }
    }
}
