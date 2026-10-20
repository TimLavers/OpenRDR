package io.rippledown.integration.proxy

import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import java.io.File
import java.time.Duration

class DirProxy(
    fromRuntime: File = File(System.getProperty("user.dir")),
    private val deleteDirectory: (File) -> Boolean = File::deleteRecursively,
    private val cleanupTimeout: Duration = Duration.ofSeconds(2)
) {
    private val userDir = if (fromRuntime.name == "cucumber") fromRuntime.parentFile else fromRuntime
    private val tempDir = File(userDir, "temp")
    private val downloadsDir = File(userDir, "downloads")
    private val logDir = File(tempDir, "logs")

    fun userDir() = userDir
    fun tempDir() = tempDir
    fun downloadsDir() = downloadsDir
    fun logDir() = logDir

    fun createAndCleanManagedDirectories() {
        check(tempDir.canonicalFile.parentFile == userDir.canonicalFile)
        check(downloadsDir.canonicalFile.parentFile == userDir.canonicalFile)
        cleanupAndCreateAnew(tempDir)
        cleanupAndCreateAnew(downloadsDir)
        check(logDir.mkdirs()) { "Could not create log directory: $logDir" }
    }

    private fun cleanupAndCreateAnew(file: File) {
        with(file) {
            if (exists()) {
                // Windows can briefly retain file handles after the server process has exited.
                try {
                    await().pollDelay(Duration.ZERO).pollInterval(Duration.ofMillis(10))
                        .atMost(cleanupTimeout).until { deleteDirectory(file) }
                } catch (timeout: ConditionTimeoutException) {
                    throw IllegalStateException(
                        "Could not delete managed directory after server exit: ${file.path}",
                        timeout
                    )
                }
            }
            mkdir()
            //sanity checks
            exists() shouldBe true
            listFiles() shouldBe emptyArray()
        }
    }
}
