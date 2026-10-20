package io.rippledown.integration.proxy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

class ServerFilesTest {
    @TempDir
    lateinit var root: File

    @Test
    fun `constructing directory proxies does not wipe live logs or downloads`() {
        // Given
        val log = File(root, "temp/logs/server.log").apply { parentFile.mkdirs(); writeText("live") }
        val download = File(root, "downloads/export.zip").apply { parentFile.mkdirs(); writeText("export") }

        // When
        val dirs = DirProxy(root)
        DirProxy(root)

        // Then
        log.readText() shouldBe "live"
        download.readText() shouldBe "export"
        dirs.logDir() shouldBe log.parentFile
    }

    @Test
    fun `explicit cleaning recreates logs and clears managed directories`() {
        // Given
        val dirs = DirProxy(root)
        File(root, "temp/old").apply { parentFile.mkdirs(); writeText("old") }
        File(root, "downloads/old").apply { parentFile.mkdirs(); writeText("old") }

        // When
        dirs.createAndCleanManagedDirectories()

        // Then
        dirs.logDir().isDirectory shouldBe true
        dirs.downloadsDir().listFiles().orEmpty().toList() shouldBe emptyList()
        File(root, "temp/old").exists() shouldBe false
    }

    @Test
    fun `failure logs contain only scenario bytes including UTF-8 text`() {
        // Given
        val source = File(root, "server.log").apply { writeText("previous scenario\n") }
        val offset = source.length()
        source.appendText("current scénario\n")
        val destination = File(root, "failure/server.log")

        // When
        copyLogTail(source, destination, offset)

        // Then
        destination.readText() shouldBe "current scénario\n"
    }

    @Test
    fun `a truncated log is copied from its new beginning`() {
        // Given
        val source = File(root, "server.log").apply { writeText("new log") }
        val destination = File(root, "failure.log")

        // When
        copyLogTail(source, destination, 1000)

        // Then
        destination.readText() shouldBe "new log"
    }

    @Test
    fun `cleanup retries transient Windows deletion failures after process exit`() {
        // Given
        File(root, "temp/old.log").apply { parentFile.mkdirs(); writeText("old") }
        var attempts = 0
        val dirs = DirProxy(root, deleteDirectory = { directory ->
            attempts++
            if (attempts < 3) false else directory.deleteRecursively()
        })

        // When
        dirs.createAndCleanManagedDirectories()

        // Then
        attempts shouldBe 3
        dirs.logDir().isDirectory shouldBe true
        File(root, "temp/old.log").exists() shouldBe false
    }

    @Test
    fun `cleanup fails within its deadline if a managed directory stays locked`() {
        // Given
        val oldLog = File(root, "temp/old.log").apply { parentFile.mkdirs(); writeText("old") }
        val dirs = DirProxy(root, deleteDirectory = { false }, cleanupTimeout = Duration.ofMillis(50))

        // When
        val failure = shouldThrow<IllegalStateException> { dirs.createAndCleanManagedDirectories() }

        // Then
        failure.message shouldContain File(root, "temp").path
        oldLog.readText() shouldBe "old"
        dirs.logDir().exists() shouldBe false
    }
}
