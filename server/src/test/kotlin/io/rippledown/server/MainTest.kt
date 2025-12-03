package io.rippledown.server

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.STARTING_SERVER
import org.apache.commons.io.FileUtils
import java.io.File
import java.time.Duration
import java.time.Duration.ofSeconds
import java.time.Instant
import kotlin.test.Test

class MainTest {

    @Test
    fun `should be able to call main() and verify that the server has started`() {
        //Given
        val logFile = File("temp/test.log") //defined in logback-test.xml
        FileUtils.forceDelete(logFile)

        //When
        val serverThread = Thread {
            main(args = arrayOf(IN_MEMORY))
        }.apply {
            isDaemon = true
            start()
        }

        //Then
        shouldNotThrowAny {
            waitFor(timeout = ofSeconds(10)) {
                logFile.exists() && logFile.readText().contains(STARTING_SERVER)
            }
        }

        // Stop the server and clean up
        runCatching { server.stop(1000, 1000) }
        serverThread.interrupt()
    }

    private fun waitFor(
        timeout: Duration = ofSeconds(5),
        checkInterval: Duration = Duration.ofMillis(100),
        condition: () -> Boolean
    ) {
        val endTime = Instant.now().plus(timeout)
        while (Instant.now().isBefore(endTime)) {
            if (condition()) {
                return
            }
            Thread.sleep(checkInterval.toMillis())
        }
        throw AssertionError("Condition not met within $timeout")
    }

}