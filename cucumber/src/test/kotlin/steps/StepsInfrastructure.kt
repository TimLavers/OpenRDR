package steps

import io.cucumber.java.Scenario
import io.rippledown.integration.UITestBase
import steps.StepsInfrastructure.client
import steps.StepsInfrastructure.uiTestBase
import java.io.File

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

    fun screenshotOnFailure(scenario: Scenario) {
        if (scenario.isFailed && ::launchedClient.isInitialized) {
            val safeName = scenario.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File("build/screenshots/${safeName}.png")
            println("Scenario failed — saving screenshot to ${file.absolutePath}")
            try {
                launchedClient.screenshot(file)
            } catch (e: Exception) {
                println("Failed to capture screenshot: ${e.message}")
            }
        }
    }

    fun cleanup() {
        if (::launchedClient.isInitialized) launchedClient.stopClient()
        uiTestBase.serverProxy.shutdown()
    }
}

fun labProxy() = uiTestBase.labProxy
fun restClient() = uiTestBase.restClient
fun applicationBarPO() = client().applicationBarPO()
fun caseListPO() = client().caseListPO()
fun cornerstoneCaseListPO() = client().cornerstoneCaseListPO()
fun caseCountPO() = client().caseCountPO()
fun kbControlsPO() = client().kbControlsPO()
fun editCurrentKbControlPO() = client().editCurrentKbControlPO()
fun caseViewPO() = client().caseViewPO()
fun cornerstonePO() = client().cornerstonePO()
fun interpretationViewPO() = client().interpretationViewPO()
fun chatPO() = client().chatPO()
fun ruleMakerPO() = client().ruleMakerPO()
