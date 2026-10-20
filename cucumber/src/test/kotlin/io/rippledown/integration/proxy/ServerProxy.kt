package io.rippledown.integration.proxy

import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.STARTING_SERVER
import io.rippledown.constants.server.TEST_RESET_FLAG
import io.rippledown.integration.restclient.RESTClient
import org.awaitility.Awaitility.await
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS

class ServerProxy(
    private val dirProxy: DirProxy = DirProxy(),
    private val restClient: RESTClient = RESTClient(),
    private val launch: (ProcessBuilder) -> Process = { it.start() }
) {
    private var process: Process? = null
    private var closed = false
    private lateinit var jarFile: File
    private val systemOutputFile = File(dirProxy.tempDir(), "output.txt")
    private val logProxy = LogProxy(dirProxy.logDir())

    fun start() = start(true)

    fun startWithPostgres() = start(false)

    @Synchronized
    fun start(inMemory: Boolean) {
        check(!closed && process == null) { "Use a new ServerProxy for a replacement process" }
        findJar()
        dirProxy.createAndCleanManagedDirectories()
        val tempDir = dirProxy.tempDir().absolutePath
        val command = listOf(
            "java",
            "-DlogFilePath=$tempDir/logs/server.log",
            "-Xmx24G",
            "--enable-native-access=ALL-UNNAMED",
            "-jar",
            jarFile.absolutePath
        ) + if (inMemory) listOf(IN_MEMORY, TEST_RESET_FLAG) else emptyList()
        process = launch(
            ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(systemOutputFile)
                .directory(dirProxy.tempDir())
        )
        waitForServerToStart()
    }
    private fun findJar() {
        val rootDirectory = dirProxy.userDir()
        val serverLibsDir = Paths.get(rootDirectory.path, "server", "build", "libs").toFile()
        jarFile = checkNotNull(serverLibsDir.listFiles { it -> it.name == "server-all.jar" }?.singleOrNull()) {
            "Build :server:shadowJar first; server-all.jar is missing from $serverLibsDir"
        }
//        println("jar: ${jarFile.absolutePath}, modified: ${Date(jarFile.lastModified())}")
    }

    private fun waitForServerToStart() {
        await().atMost(Duration.ofSeconds(30)).until {
            logProxy.contains(STARTING_SERVER) && restClient.serverIsRunning()
        }
    }

    fun tempDir() = dirProxy.tempDir()

    fun geminiCallsSoFar(): Int =
        if (logProxy.logFile.exists()) geminiCallsLogged(logProxy.logFile.readLines()) else 0

    fun isRunning() = process?.isAlive == true && restClient.serverIsRunning()

    @Synchronized
    fun shutdown() {
        if (closed) return
        val running = process
        try {
            if (running?.isAlive == true) restClient.shutdown()
        } finally {
            try {
                if (running != null) {
                    if (running.isAlive) running.destroyForcibly()
                    check(running.waitFor(10, SECONDS)) { "Server process did not exit; refusing to start another" }
                }
                closed = true
            } finally {
                restClient.close()
            }
        }
    }
    fun restClient() = restClient
}
