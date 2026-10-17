package steps

import io.rippledown.integration.UITestBase
import java.net.URI

/** Owns one process; client windows remain scenario-scoped. Also shared by the two run-cleanup hooks. */
class FeatureServerLifecycle(
    private val createBase: () -> UITestBase = ::UITestBase,
    private val reportResetFailure: (UITestBase, Exception) -> Unit = { _, error ->
        println("Replacing server after reset failed: ${error.message}")
    }
) {
    var current: UITestBase? = null
        private set
    var serverStartSeconds = 0.0
        private set
    private var feature: URI? = null
    private var postgres = false
    private var reusable = false
    private var stopped = true

    @Synchronized
    fun prepare(featureUri: URI, postgres: Boolean = false): UITestBase {
        serverStartSeconds = 0.0
        val existing = current
        if (existing != null && reusable && !postgres && !this.postgres && feature == featureUri &&
            existing.serverProxy.isRunning()
        ) {
            try {
                existing.restClient.reset()
                return existing
            } catch (e: Exception) {
                reusable = false
                try {
                    reportResetFailure(existing, e)
                } catch (diagnosticFailure: Exception) {
                    println("Could not save reset diagnostics: ${diagnosticFailure.message}; original failure: ${e.message}")
                }
            }
        }
        stop()
        val replacement = createBase()
        current = replacement
        stopped = false // includes failed startup, which still needs cleanup
        this.postgres = postgres
        val began = System.nanoTime()
        try {
            if (postgres) replacement.serverProxy.startWithPostgres() else replacement.serverProxy.start()
            feature = featureUri
            reusable = !postgres
            return replacement
        } catch (e: Exception) {
            try {
                stop()
            } catch (cleanupFailure: Exception) {
                e.addSuppressed(cleanupFailure)
            }
            throw e
        } finally {
            serverStartSeconds = (System.nanoTime() - began) / 1_000_000_000.0
        }
    }

    @Synchronized
    fun invalidate() {
        reusable = false
    }

    @Synchronized
    fun afterScenario() {
        if (postgres) stop()
    }

    @Synchronized
    fun stop() {
        reusable = false
        if (stopped) return
        current?.serverProxy?.shutdown()
        stopped = true
        feature = null
    }
}
