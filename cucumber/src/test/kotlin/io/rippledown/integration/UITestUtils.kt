package io.rippledown.integration

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS
import org.awaitility.Awaitility.await
import org.awaitility.core.ThrowingRunnable
import java.lang.Runtime.getRuntime
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
fun waitUntilAsserted(seconds: Long = 10, assertion: ThrowingRunnable) {
    await().atMost(ofSeconds(seconds)).untilAsserted(assertion)
}

fun memUsage(): String {
    System.gc()
    val free = getRuntime().freeMemory() / 1024 / 1024
    val total = getRuntime().totalMemory() / 1024 / 1024
    val max = getRuntime().maxMemory() / 1024 / 1024
    return "Free=$free, Total=$total, Max=$max MB"
}
