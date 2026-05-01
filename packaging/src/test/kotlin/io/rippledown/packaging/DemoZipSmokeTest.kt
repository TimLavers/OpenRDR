package io.rippledown.packaging

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
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
 * demoZip task.
 *
 * Two test methods, one per OS family, gated by JUnit 5 @EnabledOnOs so
 * running :packaging:test on macOS / Linux runs the unix variant and on
 * Windows runs the windows variant. They share all non-launch logic via
 * the LauncherStrategy abstraction.
 *
 * The bundled server hard-codes port 9090. If anything is already
 * listening on 9090 the test fails fast with a clear message.
 */
class DemoZipSmokeTest {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private var strategy: LauncherStrategy? = null
    private var installDir: File? = null

    @AfterEach
    fun tearDown() {
        strategy?.let { runCatching { it.stop() } }
        strategy = null
        waitForPortClosed(9090, timeoutSeconds = 20)
    }

    @Test
    @EnabledOnOs(OS.MAC, OS.LINUX)
    fun `demo zip launches working server and UI on unix`() {
        runSmoke(UnixLauncherStrategy())
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun `demo zip launches working server and UI on windows`() {
        runSmoke(WindowsLauncherStrategy())
    }

    private fun runSmoke(strat: LauncherStrategy) {
        require(!isPortOpen(9090)) {
            "Port 9090 is already in use - stop any running OpenRDR instance before running this smoke test."
        }

        val zip = locateDemoZip()
        installDir = extractZip(zip)
        val demoRoot = File(installDir, "openrdr-demo")
        demoRoot.isDirectory shouldBe true

        strat.preLaunchSanityChecks(demoRoot)

        strategy = strat
        strat.launch(demoRoot)

        waitForPortOpen(9090, timeoutSeconds = 90, strat = strat)

        httpGetBody("http://localhost:9090/api/kbList") shouldContain "\"Demo\""

        Thread.sleep(15_000)

        strat.assertUiAlive(demoRoot)

        httpGetBody("http://localhost:9090/api/kbList") shouldContain "\"Demo\""

        val serverLog = File(demoRoot, "logs/server.log")
        serverLog.shouldBeAFile()
        val serverText = serverLog.readText()
        serverText shouldContain "Starting server"
        serverText shouldContain "Demo KB seeded."
    }

    private fun locateDemoZip(): File {
        val explicit = System.getProperty("demoZip.path")?.takeIf { it.isNotBlank() }
        if (explicit != null) {
            val f = File(explicit)
            require(f.isFile) { "demoZip.path='$explicit' is not a file" }
            return f
        }
        val distDir = File(System.getProperty("user.dir"))
            .parentFile.resolve("build/distributions")
        val match = distDir.listFiles { f -> f.name.startsWith("openrdr-demo-") && f.name.endsWith(".zip") }
            ?.maxByOrNull { it.lastModified() }
        require(match != null) {
            "No openrdr-demo-*.zip found under $distDir. Run ./gradlew demoZip first or use the verifyDemoZip task."
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
        val cmd = if (isWindows()) {
            listOf(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "Expand-Archive -LiteralPath '${zip.absolutePath}' -DestinationPath '${target.absolutePath}' -Force"
            )
        } else {
            listOf("unzip", "-q", zip.absolutePath, "-d", target.absolutePath)
        }
        val proc = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .inheritIO()
            .start()
        val ok = proc.waitFor(120, TimeUnit.SECONDS)
        require(ok) { "extract timed out" }
        require(proc.exitValue() == 0) { "extract failed with exit code ${proc.exitValue()}" }
        return target
    }

    private fun isPortOpen(port: Int): Boolean = runCatching {
        Socket("localhost", port).use { true }
    }.getOrDefault(false)

    private fun waitForPortOpen(port: Int, timeoutSeconds: Int, strat: LauncherStrategy) {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            strat.assertLaunchStillProgressing(port)
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

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("windows")

    private interface LauncherStrategy {
        fun preLaunchSanityChecks(demoRoot: File)
        fun launch(demoRoot: File)
        fun assertLaunchStillProgressing(port: Int)
        fun assertUiAlive(demoRoot: File)
        fun stop()
    }

    /**
     * macOS / Linux: run start-demo.sh as a foreground process. The
     * script foregrounds the UI launcher (after backgrounding the server),
     * so the script process staying alive is direct evidence the UI did
     * not crash. Both the server and UI write captured stdout/stderr to
     * logs/server-console.log and logs/ui-console.log respectively.
     */
    private inner class UnixLauncherStrategy : LauncherStrategy {
        private var launcher: Process? = null

        override fun preLaunchSanityChecks(demoRoot: File) {
            val startScript = File(demoRoot, "start-demo.sh")
            startScript.canExecute() shouldBe true
        }

        override fun launch(demoRoot: File) {
            launcher = ProcessBuilder("bash", "./start-demo.sh")
                .directory(demoRoot)
                .redirectErrorStream(true)
                .inheritIO()
                .start()
        }

        override fun assertLaunchStillProgressing(port: Int) {
            val l = launcher ?: return
            if (!l.isAlive) {
                error("start-demo.sh exited before port $port came up (exit=${l.exitValue()})")
            }
        }

        override fun assertUiAlive(demoRoot: File) {
            withClue("start-demo.sh exited prematurely (UI launcher likely crashed)") {
                (launcher?.isAlive ?: false) shouldBe true
            }

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
        }

        override fun stop() {
            launcher?.let { p ->
                runCatching {
                    p.toHandle().descendants().forEach { it.destroyForcibly() }
                    p.destroyForcibly()
                    p.waitFor(10, TimeUnit.SECONDS)
                }
            }
            launcher = null
        }
    }

    /**
     * Windows: run start-demo.bat via cmd /c. The bat itself returns
     * after spawning two detached processes:
     *
     *   - the server, in a new console window opened by
     *     start "OpenRDR Server" cmd /k "...java -jar... InMemory Demo"
     *   - the UI, launched detached via start "" "...OpenRDR.exe"
     *
     * Because both are detached, Process.descendants does not see them,
     * so we identify and kill them via PowerShell + WMI by filtering on
     * processes whose command line references the install dir. This is
     * also how we count "still alive" processes for the UI/server checks.
     */
    private data class WinProc(val pid: Long, val name: String)

    private inner class WindowsLauncherStrategy : LauncherStrategy {
        private var batLauncher: Process? = null
        private var installRoot: File? = null

        override fun preLaunchSanityChecks(demoRoot: File) {
            File(demoRoot, "start-demo.bat").shouldBeAFile()
            File(demoRoot, "ui/OpenRDR/OpenRDR.exe").shouldBeAFile()
        }

        override fun launch(demoRoot: File) {
            installRoot = demoRoot
            batLauncher = ProcessBuilder("cmd.exe", "/c", "start-demo.bat")
                .directory(demoRoot)
                .redirectErrorStream(true)
                .inheritIO()
                .start()
        }

        override fun assertLaunchStillProgressing(port: Int) {
            // The bat exits soon after launching its detached children.
        }

        override fun assertUiAlive(demoRoot: File) {
            val procs = openRdrProcesses(demoRoot)
            withClue(
                "Expected at least one OpenRDR.exe (UI) and one java.exe (bundled JRE running the server) " +
                        "with command line referencing ${demoRoot.absolutePath}; found:\n" +
                        procs.joinToString("\n") { "  pid=${it.pid} name=${it.name}" }
            ) {
                procs.any { it.name.equals("OpenRDR.exe", ignoreCase = true) } shouldBe true
                procs.any { it.name.equals("java.exe", ignoreCase = true) } shouldBe true
            }
        }

        override fun stop() {
            batLauncher?.runCatching {
                toHandle().descendants().forEach { it.destroyForcibly() }
                destroyForcibly()
                waitFor(5, TimeUnit.SECONDS)
            }
            batLauncher = null

            installRoot?.let { root ->
                runCatching {
                    val script = """
                        ${'$'}procs = Get-CimInstance Win32_Process | Where-Object { ${'$'}_.CommandLine -and ${'$'}_.CommandLine -like '*${
                        psEscape(
                            root.absolutePath
                        )
                    }*' }
                        foreach (${'$'}p in ${'$'}procs) { Stop-Process -Id ${'$'}p.ProcessId -Force -ErrorAction SilentlyContinue }
                    """.trimIndent()
                    ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script)
                        .redirectErrorStream(true)
                        .inheritIO()
                        .start()
                        .waitFor(15, TimeUnit.SECONDS)
                }
            }
            installRoot = null
        }

        private fun openRdrProcesses(demoRoot: File): List<WinProc> {
            val script = """
                Get-CimInstance Win32_Process |
                    Where-Object { ${'$'}_.CommandLine -and ${'$'}_.CommandLine -like '*${psEscape(demoRoot.absolutePath)}*' } |
                    ForEach-Object { "${'$'}(${'$'}_.ProcessId)`t${'$'}(${'$'}_.Name)" }
            """.trimIndent()
            val proc = ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor(15, TimeUnit.SECONDS)
            return out.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    val parts = line.split('\t', limit = 2)
                    val pid = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                    val name = parts.getOrNull(1) ?: return@mapNotNull null
                    WinProc(pid, name)
                }
                .toList()
        }

        private fun psEscape(s: String): String = s.replace("'", "''")
    }
}
