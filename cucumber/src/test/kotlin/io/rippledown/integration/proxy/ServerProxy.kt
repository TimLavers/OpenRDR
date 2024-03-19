package io.rippledown.integration.proxy

import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.STARTING_SERVER
import io.rippledown.integration.restclient.RESTClient
import org.awaitility.Awaitility.await
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.util.*

class ServerProxy {
    private lateinit var process: Process
    private lateinit var jarFile: File
    private val systemOutputFile: File
    private val dirProxy = DirProxy()
    private val logProxy = LogProxy(dirProxy.logDir())
    private val restClient = RESTClient()

    init {
        systemOutputFile = File(dirProxy.tempDir(), "output.txt").apply { createNewFile() }
        dirProxy.createAndCleanManagedDirectories()
    }

    fun start() = start(true)

    fun startWithPostgres() = start(false)

    fun reStartWithPostgres() = start(false, cleanup = false)

    fun start(inMemory: Boolean, cleanup: Boolean = true) {
        println("START: inMemory = [${inMemory}], cleanup = [${cleanup}]")
        findJar()
        if (cleanup) {
            dirProxy.createAndCleanManagedDirectories()
            logProxy.deleteLogfiles()
        }
        val dbFlag = if (inMemory) IN_MEMORY else ""
        process = ProcessBuilder("java", "-jar", jarFile.absolutePath, dbFlag)
            .redirectErrorStream(true)
            .redirectOutput(systemOutputFile)
            .directory(dirProxy.tempDir())
            .start()
        waitForServerToStart()
    }

    private fun findJar() {
        val rootDirectory = dirProxy.userDir()
        println("rootDirectory: ${rootDirectory.path}")
        val serverLibsDir = Paths.get(rootDirectory.path, "server", "build", "libs").toFile()
        jarFile = serverLibsDir.listFiles { it -> it.name == "server-all.jar" }!![0]
        println("jar ${jarFile.absolutePath}, modified: ${Date(jarFile.lastModified())}")
    }

    private fun waitForServerToStart() {
        await().atMost(Duration.ofSeconds(30)).until {
            logProxy.contains(STARTING_SERVER) && restClient.serverIsRunning()
        }
    }

    fun tempDir() = dirProxy.tempDir()

    fun shutdown() {
        restClient.shutdown()
        await().atMost(Duration.ofSeconds(10)).until {
            !restClient.serverIsRunning()
        }
        if ( this::process.isInitialized) {
            process.destroyForcibly().waitFor()
        }
    }
    fun restClient() = restClient
}