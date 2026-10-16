package steps

import io.cucumber.java.Scenario
import io.rippledown.integration.UITestBase
import io.rippledown.integration.files.ScriptedKbFileDialogs
import io.rippledown.integration.proxy.copyLogTail
import io.rippledown.model.UserId
import steps.StepsInfrastructure.client
import steps.StepsInfrastructure.uiTestBase
import java.io.File

object StepsInfrastructure {
    private var scenarioLogOffset = 0L
    private var resetLogOffset = 0L
    private var scenarioFailureDirectory = File("build/failures/setup")
    private val serverLifecycle = FeatureServerLifecycle(reportResetFailure = { base, error ->
        println("Replacing server after reset failed: ${error.message}")
        val directory = File(scenarioFailureDirectory, "reset").apply { mkdirs() }
        File(directory, "reason.txt").writeText(error.stackTraceToString())
        saveLogs(base, directory, resetLogOffset)
    })
    val uiTestBase: UITestBase get() = checkNotNull(serverLifecycle.current) { "The server has not been started" }
    val serverStartSeconds: Double get() = serverLifecycle.serverStartSeconds

    init {
        Runtime.getRuntime().addShutdownHook(Thread({
            runCatching { stopServer() }.onFailure { System.err.println("Server shutdown failed: ${it.message}") }
        }, "cucumber-server-cleanup"))
    }

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

    fun prepareScenario(scenario: Scenario, postgres: Boolean = false) {
        stopClients() // retries any client whose previous teardown failed
        scenarioFailureDirectory = failureDir(scenario)
        scenarioLogOffset = 0
        geminiCallsAtScenarioStart = 0
        resetLogOffset = serverLifecycle.current?.let { logFile(it).length() } ?: 0
        serverLifecycle.prepare(scenario.uri, postgres)
        markScenarioStart()
    }

    // The log is wiped when a server is started for a scenario, so this reading is normally 0;
    // taking it rather than assuming it is what lets a shared server be measured the same way.
    private var geminiCallsAtScenarioStart = 0

    private fun markScenarioStart() {
        geminiCallsAtScenarioStart = uiTestBase.serverProxy.geminiCallsSoFar()
        scenarioLogOffset = logFile(uiTestBase).length()
    }

    fun geminiCallsThisScenario(): Int =
        serverLifecycle.current?.let { it.serverProxy.geminiCallsSoFar() - geminiCallsAtScenarioStart } ?: 0

    fun stopServer() {
        serverLifecycle.stop()
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
        var failure: Exception? = null
        launchedClients.toMap().forEach { (name, client) ->
            try {
                client.stopClient()
                launchedClients.remove(name)
                if (currentClient === client) currentClient = null
            } catch (e: Exception) {
                val previous = failure
                if (previous == null) failure = e else previous.addSuppressed(e)
            }
        }
        failure?.let { serverLifecycle.invalidate(); throw it }
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
        if (!scenario.isFailed) return
        val base = serverLifecycle.current ?: return
        saveLogs(base, failureDir(scenario), scenarioLogOffset)
    }

    private fun logFile(base: UITestBase) = File(base.serverProxy.tempDir(), "logs/server.log")

    private fun saveLogs(base: UITestBase, targetDir: File, logOffset: Long) {
        val tempDir = base.serverProxy.tempDir()
        val sources = listOf(
            File(tempDir, "logs/server.log"),
            File(tempDir, "output.txt")
        )
        sources.forEach { src ->
            if (!src.exists()) return@forEach
            try {
                val dst = File(targetDir, src.name)
                if (src.name == "server.log") copyLogTail(src, dst, logOffset)
                else src.copyTo(dst, overwrite = true)
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
        try {
            stopClients()
        } finally {
            useFakeVoice = false
            fileDialogs = null
            serverLifecycle.afterScenario()
        }
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
