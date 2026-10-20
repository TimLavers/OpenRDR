package steps

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.integration.UITestBase
import io.rippledown.integration.proxy.ServerProxy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.net.URI

class FeatureServerLifecycleTest {
    private val first = URI("file:///requirements/kb/first.feature")
    private val second = URI("file:///requirements/kb/second.feature")

    private fun base(): UITestBase {
        val server = mockk<ServerProxy>()
        every { server.isRunning() } returns true
        every { server.tempDir() } returns File("unused-test-temp")
        return UITestBase(server)
    }

    @Test
    fun `scenarios in one feature reset and reuse the same process`() {
        // Given
        val base = base()
        val lifecycle = FeatureServerLifecycle(createBase = { base })

        // When
        lifecycle.prepare(first)
        lifecycle.prepare(first)

        // Then
        verify(exactly = 1) { base.serverProxy.start() }
        verify(exactly = 1) { base.restClient.reset() }
        verify(exactly = 0) { base.serverProxy.shutdown() }
        lifecycle.current shouldBe base
        lifecycle.serverStartSeconds shouldBe 0.0
    }

    @Test
    fun `a feature change stops the old process before constructing its replacement`() {
        // Given
        val old = base()
        val replacement = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returnsMany listOf(old, replacement)
        val lifecycle = FeatureServerLifecycle(factory)
        lifecycle.prepare(first)
        clearMocks(factory, old.serverProxy, answers = false)

        // When
        lifecycle.prepare(second)

        // Then
        verifyOrder { old.serverProxy.shutdown(); factory(); replacement.serverProxy.start() }
        verify(exactly = 0) { old.restClient.reset() }
    }

    @Test
    fun `reset failure is diagnosed before stopping and replacing the process`() {
        // Given
        val old = base()
        val replacement = base()
        val factory = mockk<() -> UITestBase>()
        val report = mockk<(UITestBase, Exception) -> Unit>()
        every { factory() } returnsMany listOf(old, replacement)
        val error = IllegalStateException("Reset timed out")
        every { old.restClient.reset() } throws error
        val lifecycle = FeatureServerLifecycle(factory, report)
        lifecycle.prepare(first)

        // When
        lifecycle.prepare(first)

        // Then
        verifyOrder {
            old.restClient.reset(); report(
            old,
            error
        ); old.serverProxy.shutdown(); replacement.serverProxy.start()
        }
        lifecycle.current shouldBe replacement
    }

    @Test
    fun `failed termination prevents creating a replacement`() {
        // Given
        val base = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returns base
        val lifecycle = FeatureServerLifecycle(factory)
        lifecycle.prepare(first)
        every { base.serverProxy.shutdown() } throws IllegalStateException("Still alive")

        // When
        shouldThrow<IllegalStateException> { lifecycle.prepare(second) }

        // Then
        verify(exactly = 1) { factory() }
    }

    @Test
    fun `Postgres scenarios always get their own process`() {
        // Given
        val firstBase = base()
        val secondBase = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returnsMany listOf(firstBase, secondBase)
        val lifecycle = FeatureServerLifecycle(factory)

        // When
        lifecycle.prepare(first, postgres = true)
        lifecycle.afterScenario()
        lifecycle.prepare(first, postgres = true)
        lifecycle.afterScenario()

        // Then
        verify(exactly = 1) { firstBase.serverProxy.startWithPostgres(); secondBase.serverProxy.startWithPostgres() }
        verify(exactly = 1) { firstBase.serverProxy.shutdown(); secondBase.serverProxy.shutdown() }
        verify(exactly = 0) { firstBase.restClient.reset(); secondBase.restClient.reset() }
    }

    @Test
    fun `run cleanup is safe before startup and idempotent after it`() {
        // Given
        val base = base()
        val lifecycle = FeatureServerLifecycle({ base })
        lifecycle.stop()
        lifecycle.prepare(first)

        // When
        lifecycle.stop()
        lifecycle.stop()

        // Then
        verify(exactly = 1) { base.serverProxy.shutdown() }
    }

    @Test
    fun `failed startup still cleans up its process`() {
        // Given
        val base = base()
        every { base.serverProxy.start() } throws IllegalStateException("Startup failed")
        val lifecycle = FeatureServerLifecycle({ base })

        // When
        shouldThrow<IllegalStateException> { lifecycle.prepare(first) }
        lifecycle.stop()

        // Then
        verify(exactly = 1) { base.serverProxy.shutdown() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `an unhealthy or invalidated server is replaced without reset`(invalidated: Boolean) {
        // Given
        val old = base()
        val replacement = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returnsMany listOf(old, replacement)
        val lifecycle = FeatureServerLifecycle(factory)
        lifecycle.prepare(first)

        // When
        if (invalidated) lifecycle.invalidate() else every { old.serverProxy.isRunning() } returns false
        lifecycle.prepare(first)

        // Then
        verify(exactly = 0) { old.restClient.reset() }
        verify(exactly = 1) { old.serverProxy.shutdown(); replacement.serverProxy.start() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `changing persistence mode within one feature replaces the process`(startsWithPostgres: Boolean) {
        // Given
        val old = base()
        val replacement = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returnsMany listOf(old, replacement)
        val lifecycle = FeatureServerLifecycle(factory)
        lifecycle.prepare(first, postgres = startsWithPostgres)

        // When
        lifecycle.prepare(first, postgres = !startsWithPostgres)

        // Then
        verifyOrder { old.serverProxy.shutdown(); factory() }
        verify(exactly = 0) { old.restClient.reset() }
        if (startsWithPostgres) verify(exactly = 1) { replacement.serverProxy.start() }
        else verify(exactly = 1) { replacement.serverProxy.startWithPostgres() }
        lifecycle.current shouldBe replacement
    }

    @Test
    fun `a diagnostic failure cannot prevent replacement after a failed reset`() {
        // Given
        val old = base()
        val replacement = base()
        val factory = mockk<() -> UITestBase>()
        every { factory() } returnsMany listOf(old, replacement)
        every { old.restClient.reset() } throws IllegalStateException("Reset failed")
        val lifecycle = FeatureServerLifecycle(factory) { _, _ -> error("Cannot write diagnostics") }
        lifecycle.prepare(first)

        // When
        lifecycle.prepare(first)

        // Then
        verifyOrder { old.serverProxy.shutdown(); replacement.serverProxy.start() }
        lifecycle.current shouldBe replacement
    }
}
