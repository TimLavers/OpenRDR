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

    fun start() {
        findJar()
        dirProxy.createAndCleanManagedDirectories()
        logProxy.deleteLogfiles()
//        process = ProcessBuilder("java", "-jar", jarFile.absolutePath)
        process = ProcessBuilder("java", "-jar", jarFile.absolutePath, IN_MEMORY)
            .redirectErrorStream(true)
            .redirectOutput(systemOutputFile)
            .directory(dirProxy.tempDir())
            .start()
        waitForServerToStart()
    }

    private fun findJar() {
        val rootDirectory = dirProxy.userDir()
        val libsDir = Paths.get(rootDirectory.path, "build", "libs").toFile()
        jarFile = libsDir.listFiles { it -> it.name == "OpenRDR-all.jar" }!![0]
        println("jar ${jarFile.absolutePath}, modified: ${Date(jarFile.lastModified())}")
    }

    private fun waitForServerToStart() {
        await().atMost(Duration.ofSeconds(30)).until {
            logProxy.contains(STARTING_SERVER) && restClient.serverHasStarted()
        }
    }

    fun tempDir() = dirProxy.tempDir()

    fun shutdown() {
        restClient.shutdown()
        if ( this::process.isInitialized) {
            process.destroyForcibly().waitFor()
        }
    }
}