package io.rippledown.integration

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

fun stop() {
    Thread.sleep(120_000_000)
}

fun pause() = pause(1000)

fun pause(millis: Long) {
    Thread.sleep(millis)
}

fun waitForDebounce() {
    pause(DEBOUNCE_WAIT_PERIOD_MILLIS + 100)
}

fun waitUntilAssertedOnEventThread(seconds: Long = 10, assertion: () -> Unit) {
    await().atMost(ofSeconds(seconds)).untilAsserted {
        execute { assertion() }
    }
}
