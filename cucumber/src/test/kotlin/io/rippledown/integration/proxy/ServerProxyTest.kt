package io.rippledown.integration.proxy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.rippledown.constants.server.STARTING_SERVER
import io.rippledown.constants.server.TEST_RESET_FLAG
import io.rippledown.integration.restclient.RESTClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

class ServerProxyTest {
    @TempDir
    lateinit var root: File
    private val rest = mockk<RESTClient>()
    private val process = mockk<Process>()

    private fun startedProxy(): ServerProxy {
        File(root, "server/build/libs/server-all.jar").apply { parentFile.mkdirs(); writeText("test") }
        every { rest.serverIsRunning() } returns true
        every { process.isAlive } returns true
        every { process.waitFor(10, SECONDS) } returns true
        val dirs = DirProxy(root)
        return ServerProxy(dirs, rest) { builder ->
            builder.command() shouldContain TEST_RESET_FLAG
            File(dirs.logDir(), "server.log").writeText(STARTING_SERVER)
            process
        }.apply { start() }
    }

    @Test
    fun `failed graceful shutdown still destroys and waits for the owned process`() {
        // Given
        val proxy = startedProxy()
        every { rest.shutdown() } throws IllegalStateException("Transport failed")

        // When
        shouldThrow<IllegalStateException> { proxy.shutdown() }
        proxy.shutdown()

        // Then
        verifyOrder { rest.shutdown(); process.destroyForcibly(); process.waitFor(10, SECONDS); rest.close() }
        verify(exactly = 1) { rest.close(); process.destroyForcibly() }
    }

    @Test
    fun `process that will not exit causes shutdown to fail and remains retryable`() {
        // Given
        val proxy = startedProxy()
        every { process.waitFor(10, SECONDS) } returns false

        // When / Then
        shouldThrow<IllegalStateException> { proxy.shutdown() }

        // When
        every { process.waitFor(10, SECONDS) } returns true
        proxy.shutdown()

        // Then
        verify(exactly = 2) { process.destroyForcibly() }
    }

    @Test
    fun `cleanup without a started process only closes clients once`() {
        // Given
        val proxy = ServerProxy(DirProxy(root), rest)

        // When
        proxy.shutdown()
        proxy.shutdown()

        // Then
        verify(exactly = 0) { rest.shutdown() }
        verify(exactly = 1) { rest.close() }
        root.listFiles().orEmpty().size shouldBe 0
    }
}
