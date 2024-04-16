package io.rippledown.integration

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

fun stop() {
    Thread.sleep(120_000_000)
}

fun pause() = pause(1000)

fun pause(period: Long) {
    Thread.sleep(period)
}

fun waitForDebounce() {
    pause(2 * DEBOUNCE_WAIT_PERIOD_MILLIS)
}

fun waitUntilAssertedOnEventThread(assertion: () -> Unit) {
    await().atMost(ofSeconds(10)).untilAsserted {
        execute { assertion() }
    }
}
