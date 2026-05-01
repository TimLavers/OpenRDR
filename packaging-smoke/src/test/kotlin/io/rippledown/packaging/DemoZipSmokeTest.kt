package io.rippledown.packaging

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.io.File
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * End-to-end smoke test for the zip distribution produced by the root
 * `demoZip` task.
 *
 * What it verifies:
 *   1. A zip exists at `demoZip.path` (passed in by the `verifyDemoZip`
 *      Gradle task).
 *   2. It extracts cleanly with `unzip` (which preserves the unix
 *      permissions Gradle stamped on the launcher binaries and start
 *      scripts).
 *   3. `start-demo.sh` runs to completion past its own server-up wait,
 *      i.e. the bundled fat jar boots under the bundled JRE.
 *   4. The server answers an `/api/kbList` request and includes the seeded
 *      `Demo` KB.
 *   5. After ~15 s the UI launcher process is still alive (it did not
 *      crash on startup) and `logs/ui-console.log` shows no exception
 *      stack trace.
 *   6. The server still responds to a second `/api/kbList` call after the
 *      UI has come up (proving they coexist on port 9090).
 *
 * Caveats:
 *   - Skipped on Windows: the .bat launcher uses `start "" cmd /k ...` to
 *     spawn the server in a new console window, which complicates
 *     deterministic teardown. The recently-fixed packaging bugs were all
 *     on the unix side.
 *   - The bundled server hard-codes port 9090. If anything is already
 *     listening on 9090 the test fails fast with a clear message rather
 *     than producing confusing downstream errors.
 */
class DemoZipSmokeTest {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private var launcher: Process? = null
    private var installDir: File? = null

    @AfterEach
    fun tearDown() {
        launcher?.let { p ->
            // Kill the whole tree: the start script foregrounds the UI launcher
            // which in turn forks the bundled JRE for the server (server is
            // started in the background by the script with a trap-based
            // cleanup, but we want to be defensive).
            runCatching {
                p.toHandle().descendants().forEach { it.destroyForcibly() }
                p.destroyForcibly()
                p.waitFor(10, TimeUnit.SECONDS)
            }
        }
        launcher = null
        // Wait for port 9090 to be free again so a follow-up run isn't
        // blocked by a lingering server.
        waitForPortClosed(9090, timeoutSeconds = 15)
    }

    @Test
    fun `demo zip launches working server and UI`() {
        assumeFalse(
            System.getProperty("os.name").lowercase().contains("windows"),
            "Smoke test only runs on macOS / Linux for now"
        )

        require(!isPortOpen(9090)) {
            "Port 9090 is already in use - stop any running OpenRDR instance before running this smoke test."
        }

        val zip = locateDemoZip()
        installDir = extractZip(zip)
        val demoRoot = File(installDir, "openrdr-demo")
        demoRoot.isDirectory shouldBe true

        // Sanity: the start script and the launcher binary survived the
        // round-trip with executable permissions.
        val startScript = File(demoRoot, "start-demo.sh")
        startScript.canExecute() shouldBe true

        launcher = startLauncher(demoRoot)

        waitForPortOpen(9090, timeoutSeconds = 90)

        // 1. Server answers and has the seeded Demo KB.
        httpGetBody("http://localhost:9090/api/kbList") shouldContain "\"Demo\""

        // 2. Give the UI ~15 s to fully come up.
        Thread.sleep(15_000)

        // 3. Launcher script still alive => UI launcher (which it foregrounds)
        //    has not crashed.
        launcher!!.isAlive shouldBe true

        // 4. UI console log exists and contains no exception trace.
        val uiLog = File(demoRoot, "logs/ui-console.log")
        uiLog.shouldBeAFile()
        val uiText = uiLog.readText()
        val errorLines = uiText.lineSequence().filter { line ->
            line.contains("Exception") ||
                    line.contains("\tat ") ||
                    line.startsWith("ERROR") ||
                    line.contains("FATAL")
        }.toList()
        withClue("UI log contains error lines:\n${errorLines.joinToString("\n")}\n--- full log ---\n$uiText") {
            errorLines.shouldHaveSize(0)
        }

        // 5. Server still serves after UI startup.
        httpGetBody("http://localhost:9090/api/kbList") shouldContain "\"Demo\""

        // 6. Server log shows the expected boot markers (proves the bundled
        //    JRE actually loaded our fat jar, not just any java).
        val serverLog = File(demoRoot, "logs/server.log")
        serverLog.shouldBeAFile()
        val serverText = serverLog.readText()
        serverText shouldContain "Starting server"
        serverText shouldContain "Demo KB seeded."
    }

    // -------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------

    private fun locateDemoZip(): File {
        val explicit = System.getProperty("demoZip.path")?.takeIf { it.isNotBlank() }
        if (explicit != null) {
            val f = File(explicit)
            require(f.isFile) { "demoZip.path='$explicit' is not a file" }
            return f
        }
        // Fallback: glob the root distributions dir. Useful when running the
        // test directly from an IDE.
        val distDir = File(System.getProperty("user.dir"))
            .parentFile.resolve("build/distributions")
        val match = distDir.listFiles { f -> f.name.startsWith("openrdr-demo-") && f.name.endsWith(".zip") }
            ?.maxByOrNull { it.lastModified() }
        require(match != null) {
            "No openrdr-demo-*.zip found under $distDir. Run `./gradlew demoZip` first or use the verifyDemoZip task."
        }
        return match
    }

    private fun extractZip(zip: File): File {
        val rootProp = System.getProperty("demoZip.extractRoot")
            ?: error("demoZip.extractRoot not set")
        val target = File(rootProp, "run-${System.currentTimeMillis()}").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        val proc = ProcessBuilder("unzip", "-q", zip.absolutePath, "-d", target.absolutePath)
            .redirectErrorStream(true)
            .inheritIO()
            .start()
        val ok = proc.waitFor(60, TimeUnit.SECONDS)
        require(ok) { "unzip timed out" }
        require(proc.exitValue() == 0) { "unzip failed with exit code ${proc.exitValue()}" }
        return target
    }

    private fun startLauncher(demoRoot: File): Process {
        // Run the script the user actually runs. inheritIO() pipes its
        // stdout/stderr to the test's so the Gradle test report captures
        // everything the colleague would see.
        return ProcessBuilder("bash", "./start-demo.sh")
            .directory(demoRoot)
            .redirectErrorStream(true)
            .inheritIO()
            .start()
    }

    private fun isPortOpen(port: Int): Boolean = runCatching {
        Socket("localhost", port).use { true }
    }.getOrDefault(false)

    private fun waitForPortOpen(port: Int, timeoutSeconds: Int) {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            if (launcher?.isAlive == false) {
                error("start-demo.sh exited before port $port came up (exit=${launcher?.exitValue()})")
            }
            if (isPortOpen(port)) return
            Thread.sleep(500)
        }
        error("Port $port did not open within ${timeoutSeconds}s")
    }

    private fun waitForPortClosed(port: Int, timeoutSeconds: Int) {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            if (!isPortOpen(port)) return
            Thread.sleep(250)
        }
    }

    private fun httpGetBody(url: String): String {
        val req = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        withClue("GET $url -> ${resp.statusCode()}: ${resp.body()}") {
            resp.statusCode() shouldBe 200
        }
        return resp.body()
    }
}
