package io.rippledown.casecontrol

import androidx.compose.ui.test.junit4.createComposeRule
import io.rippledown.main.Api
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.mocks.config
import io.rippledown.mocks.mock
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.createCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class CasePollerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should get the number of cases`() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            requireNumberOfCases(3)
        }
    }

    @Test
    fun `should show names on the case list`() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            requireNumberOfCases(2)
            requireNamesToBeShowingOnCaseList(case1, case2)
        }
    }

    @Test
    fun `should select the first case on the case list`() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            requireCaseToBeShowing(case1)
        }
    }

    @Test
    fun `should not show case selector when there are no more cases`() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            //Given
            requireNumberOfCases(3)

            //When
            config.returnCasesInfo = CasesInfo(emptyList())

            //Then
            requireCaseSelectorNotToBeShowing()
        }
    }


    /*
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
        */
}
