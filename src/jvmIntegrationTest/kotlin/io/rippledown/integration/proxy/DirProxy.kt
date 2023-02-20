package io.rippledown.integration.proxy

import io.kotest.matchers.shouldBe
import java.io.File

class DirProxy {
    private lateinit var userDir: File
    private lateinit var tempDir: File
    private lateinit var downloadsDir: File
    private var logDir: File

    init {
        createAndCleanManagedDirectories()
        logDir = File(tempDir, "logs").apply { mkdir() }
    }

    fun userDir() = userDir
    fun tempDir() = tempDir
    fun downloadsDir() = downloadsDir
    fun logDir() = logDir

    fun createAndCleanManagedDirectories() {
        userDir = File(System.getProperty("user.dir"))
        tempDir = File(userDir, "temp")
        cleanupAndCreateAnew(tempDir)
        downloadsDir = File(userDir, "downloads")
        cleanupAndCreateAnew(downloadsDir)
    }

    private fun cleanupAndCreateAnew(file: File) {
        with(file) {
            if (exists()) {
                val deleted = deleteRecursively()
                if (!deleted) {
                    throw RuntimeException("Could not delete temp dir: ${file.path}. Enter \"tskill -f java\" and try again.")
                }
            }
            mkdir()
            //sanity checks
            exists() shouldBe true
            listFiles() shouldBe emptyArray()
        }
    }
}