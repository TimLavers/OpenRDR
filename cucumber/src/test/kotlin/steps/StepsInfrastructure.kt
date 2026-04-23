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
            val file = File(failureDir(scenario), "screenshot.png")
            println("Scenario failed - saving screenshot to ${file.absolutePath}")
            try {
                launchedClient.screenshot(file)
            } catch (e: Exception) {
                println("Failed to capture screenshot: ${e.message}")
            }
        }
    }

    fun saveServerLogsOnFailure(scenario: Scenario) {
        if (!scenario.isFailed || !::uiTestBase.isInitialized) return
        val tempDir = uiTestBase.serverProxy.tempDir()
        val sources = listOf(
            File(tempDir, "logs/server.log"),
            File(tempDir, "output.txt")
        )
        val targetDir = failureDir(scenario)
        sources.forEach { src ->
            if (!src.exists()) return@forEach
            try {
                val dst = File(targetDir, src.name)
                src.copyTo(dst, overwrite = true)
                println("Scenario failed - saved ${src.name} to ${dst.absolutePath}")
            } catch (e: Exception) {
                println("Failed to copy ${src.name}: ${e.message}")
            }
        }
    }

    private fun failureDir(scenario: Scenario): File {
        val safeName = scenario.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File("build/failures/$safeName").apply { mkdirs() }
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
