package io.rippledown.integration.proxy

import io.kotest.matchers.shouldBe
import java.io.File

class DirProxy {
    private lateinit var userDir: File
    private lateinit var tempDir: File
    private var logDir: File

    init {
        createAndCleanTempDir()
        logDir = File(tempDir, "logs").apply { mkdir() }
    }

    fun userDir() = userDir
    fun tempDir() = tempDir
    fun logDir() = logDir

    fun createAndCleanTempDir() {
        userDir = File(System.getProperty("user.dir"))
        tempDir = File(userDir, "temp")
        with(tempDir) {
            if (exists()) {
                val deleted = deleteRecursively()
                if (!deleted) {
                    throw RuntimeException("Could not delete temp dir: ${tempDir.path}. Enter \"tskill -f java\" and try again.")
                }
            }
            mkdir()
            //sanity checks
            exists() shouldBe true
            listFiles() shouldBe emptyArray()
        }
    }
}