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
    fun kbControlsPO() = rdUiOperator.kbControlsPO()
    fun editCurrentKbControlPO() = rdUiOperator.editCurrentKbControlPO()
    fun caseViewPO() = rdUiOperator.caseViewPO()
    fun cornerstonePO() = rdUiOperator.cornerstonePO()
    fun interpretationViewPO() = rdUiOperator.interpretationViewPO()
    fun chatPO() = rdUiOperator.chatPO()
    fun ruleMakerPO() = rdUiOperator.ruleMakerPO()
    fun stopClient() {
        testClientLauncher.stopClient()
    }
}