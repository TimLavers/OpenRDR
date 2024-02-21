package steps

import io.rippledown.TestClientLauncher
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.*
import steps.StepsInfrastructure.client
import steps.StepsInfrastructure.uiTestBase

object StepsInfrastructure {
    var uiTestBase: UITestBase? = null
    private var launchedClient: LaunchedClient? = null

    fun startServerWithInMemoryDatabase() {
        setup()
        uiTestBase!!.serverProxy.start()
        uiTestBase!!.restClient.createKBWithDefaultName()
    }

    fun startServerWithPostgresDatabase() {
        setup()
        uiTestBase!!.serverProxy.startWithPostgres()
    }

    fun startClient() {
        require(launchedClient == null) {
            "LaunchedClient not null. Was the previous test cleaned up?"
        }
        launchedClient = LaunchedClient()
    }

    fun client(): LaunchedClient { require(launchedClient != null) {
            "Was the client launched?"
        }
        return launchedClient!!
    }

    fun cleanup() {
        launchedClient?.stopClient()
        launchedClient = null
        uiTestBase!!.serverProxy.shutdown()
        uiTestBase = null
    }

    private fun setup() {
        require(uiTestBase == null) {
            "UITestBase not null. Did the previous test clean up?"
        }
        uiTestBase = UITestBase()
    }
}
class LaunchedClient {
    val testClientLauncher = TestClientLauncher()
    val composeWindow = testClientLauncher.launchClient()
    val rdUiOperator = RippleDownUIOperator(composeWindow)
    val caseListPO = rdUiOperator.caseListPO()
    val caseViewPO = rdUiOperator.caseViewPO()
    val interpretationViewPO = rdUiOperator.interpretationViewPO()

    fun stopClient() {
        testClientLauncher.stopClient()
    }
}
fun caseListPO() = client().caseListPO
fun caseViewPO() = client().caseViewPO
fun labProxy() = uiTestBase!!.labProxy
fun interpretationViewPO() = client().interpretationViewPO