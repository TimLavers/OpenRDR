package steps

import io.rippledown.integration.UITestBase
import steps.StepsInfrastructure.client
import steps.StepsInfrastructure.uiTestBase

object StepsInfrastructure {
    lateinit var uiTestBase: UITestBase
    private lateinit var launchedClient: LaunchedClient
    private fun setup() {
        uiTestBase = UITestBase()
    }

    fun startServerWithInMemoryDatabase() {
        setup()
        uiTestBase.serverProxy.start()
        uiTestBase.restClient.createKBWithDefaultName()
    }

    fun startServerWithPostgresDatabase() {
        setup()
        uiTestBase.serverProxy.startWithPostgres()
    }

    fun reStartWithPostgres() {
        uiTestBase.serverProxy.reStartWithPostgres()
    }

    fun stopServer() {
        uiTestBase.serverProxy.shutdown()
    }

    fun startClient() {
        launchedClient = LaunchedClient()
    }
    fun client() = launchedClient

    fun cleanup() {
        if (::launchedClient.isInitialized) launchedClient.stopClient()
        uiTestBase.serverProxy.shutdown()
    }
}

fun labProxy() = uiTestBase.labProxy
fun restClient() = uiTestBase.restClient
fun applicationBarPO() = client().applicationBarPO()
fun caseListPO() = client().caseListPO()
fun caseCountPO() = client().caseCountPO()
fun kbControlsPO() = client().kbControlsPO()
fun caseViewPO() = client().caseViewPO()
fun cornerstonePO() = client().cornerstonePO()
fun interpretationViewPO() = client().interpretationViewPO()
fun ruleMakerPO() = client().ruleMakerPO()
