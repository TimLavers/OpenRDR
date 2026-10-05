package steps

import io.cucumber.java.Scenario
import io.rippledown.integration.UITestBase
import io.rippledown.integration.files.ScriptedKbFileDialogs
import io.rippledown.model.UserId
import steps.StepsInfrastructure.client
import steps.StepsInfrastructure.uiTestBase
import java.io.File

object StepsInfrastructure {
    lateinit var uiTestBase: UITestBase

    /**
     * The running clients by user name, and the one the page objects address.
     * The single-user scenarios use one unnamed client; the two-window
     * scenarios start one per user and switch between them.
     */
    private val launchedClients = mutableMapOf<String?, LaunchedClient>()
    private var currentClient: LaunchedClient? = null

    /**
     * Per-scenario flag set by the `@voice-is-fake` cucumber tag. When
     * true, [LaunchedClient] installs a [io.rippledown.integration.FakeVoiceRecognition]
     * so steps can drive the chat panel via `simulateUtterance`. When
     * false (the default), the running UI uses the real microphone +
     * Gemini transcription pipeline, which is what you want for paused
     * free-play scenarios.
     */
    var useFakeVoice: Boolean = false
    var fileDialogs: ScriptedKbFileDialogs? = null

    private fun setup() {
        uiTestBase = UITestBase()
    }

    fun startServerWithInMemoryDatabase() {
        setup()
        uiTestBase.serverProxy.start()
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
        // A scenario that starts the client twice would otherwise leave the first
        // window running and overlapping the second: the page objects address the
        // new window's accessibility tree while native focus, and so every Robot
        // keystroke, can go to the old one.
        stopClients()
        startClient(null, LaunchedClient())
    }

    fun startClientFor(userName: String) {
        launchedClients[userName]?.stopClient()
        startClient(userName, LaunchedClient(UserId(userName)))
    }

    private fun startClient(userName: String?, client: LaunchedClient) {
        launchedClients[userName] = client
        currentClient = client
    }

    // Windows overlap, and Robot clicks land on whichever is in front, so
    // switching brings the window forward as well as redirecting the page objects.
    fun switchToClientOf(userName: String) {
        val client = launchedClients[userName] ?: error("$userName has not started the client application.")
        currentClient = client
        client.bringToFront()
    }

    fun client() = currentClient ?: error("The client application has not been started.")

    private fun stopClients() {
        launchedClients.values.forEach { it.stopClient() }
        launchedClients.clear()
        currentClient = null
    }

    fun screenshotOnFailure(scenario: Scenario) {
        val running = currentClient
        if (scenario.isFailed && running != null) {
            val file = File(failureDir(scenario), "screenshot.png")
            println("Scenario failed - saving screenshot to ${file.absolutePath}")
            try {
                running.screenshot(file)
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
        stopClients()
        uiTestBase.serverProxy.shutdown()
        useFakeVoice = false
        fileDialogs = null
    }
}

fun labProxy() = uiTestBase.labProxy
fun restClient() = uiTestBase.restClient
fun applicationBarPO() = client().applicationBarPO()
fun caseListPO() = client().caseListPO()
fun cornerstoneCaseListPO() = client().cornerstoneCaseListPO()
fun processedCaseListPO() = client().processedCaseListPO()
fun favouriteCaseListPO() = client().favouriteCaseListPO()
fun userDefinedCaseListPO(name: String) = client().userDefinedCaseListPO(name)
fun caseCountPO() = client().caseCountPO()
fun cornerstoneCaseCountPO() = client().cornerstoneCaseCountPO()
fun userDefinedCaseListCaseCountPO(name: String) = client().userDefinedCaseListCaseCountPO(name)
fun kbControlsPO() = client().kbControlsPO()
fun caseViewPO() = client().caseViewPO()
fun cornerstonePO() = client().cornerstonePO()
fun interpretationViewPO() = client().interpretationViewPO()
fun chatPO() = client().chatPO()
fun reportPO() = client().reportPO()
