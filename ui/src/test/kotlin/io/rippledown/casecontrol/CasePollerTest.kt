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
import kotlin.test.Ignore
import kotlin.test.Test

class CasePollerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show the number of cases`() = runTest {
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
            waitForNumberOfCases(3)
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
            waitForNumberOfCases(2)
            requireNamesToBeShowingOnCaseList(case1, case2)
        }
    }

    @Test
    fun `should select the first case on the case list by default`() = runTest {
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
            waitForCaseToBeShowing(case1)
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
            waitForNumberOfCases(3)

            //When
            config.returnCasesInfo = CasesInfo(emptyList())

            //Then
            waitForCaseSelectorNotToBeShowing()
        }
    }


    @Test
    fun `should show a case view when a case is selected`() = runTest {
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

        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }

            //Given
            waitForNumberOfCases(2)
            requireNamesToBeShowingOnCaseList(caseName1, caseName2)
            waitForCaseToBeShowing(caseName1)

            //When
            config.returnCase = createCase(caseId2)
            selectCaseByName(caseName2)

            //Then
            waitForCaseToBeShowing(caseName2)
        }
    }


    @Test
    fun `should update the number of cases`() = runTest {
        val caseId1 = CaseId(1, "case 1")
        val caseId2 = CaseId(2, "case 2")
        val config = config {
            returnCasesInfo = CasesInfo(listOf(caseId1))
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

            //Given
            waitForNumberOfCases(1)

            //When
            config.returnCasesInfo = CasesInfo(listOf(caseId1, caseId2))

            //Then
            waitForNumberOfCases(2)
        }
    }

    @Test
    fun `should select the first case when the selected case has been deleted`() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            //Given
            selectCaseByName(caseName2)
            waitForCaseToBeShowing(caseName2)

            //When
            //set the mock to return only the other two cases
            config.returnCasesInfo = CasesInfo(
                listOf(caseId1, caseId3)
            )
            config.returnCase = createCase(caseId1)

            //Then
            waitForCaseToBeShowing(caseName1)
            requireNamesToBeShowingOnCaseList(caseName1, caseName3)
        }
    }

    @Test
    fun shouldSelectTheFirstCaseByDefault() = runTest {
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
        with(composeTestRule) {
            setContent {
                CasePoller(object : Handler by handlerImpl, CasePollerHandler {
                    override var api = Api(mock(config))
                    override var isRuleSessionInProgress = false
                    override var setRuleInProgress: (Boolean) -> Unit = {}
                })
            }
            waitForCaseToBeShowing(case1)
        }
    }
}
