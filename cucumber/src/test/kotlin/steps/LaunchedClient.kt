package steps

import io.rippledown.TestClientLauncher
import io.rippledown.integration.pageobjects.RippleDownUIOperator

class LaunchedClient {
    private val testClientLauncher = TestClientLauncher()
    private val composeWindow = testClientLauncher.launchClient()
    private val rdUiOperator = RippleDownUIOperator(composeWindow)
    fun applicationBarPO() = rdUiOperator.applicationBarOperator()
    fun caseListPO() = rdUiOperator.caseListPO()
    fun caseCountPO() = rdUiOperator.caseCountPO()
    fun caseViewPO() = rdUiOperator.caseViewPO()
    fun interpretationViewPO() = rdUiOperator.interpretationViewPO()
    fun conclusionsViewPO() = rdUiOperator.conclusionsViewPO()

    fun stopClient() {
        testClientLauncher.stopClient()
    }
}