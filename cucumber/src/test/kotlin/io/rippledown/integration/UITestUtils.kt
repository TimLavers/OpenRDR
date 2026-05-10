package io.rippledown.integration

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

fun waitUntilAsserted(seconds: Long = 60, assertion: ThrowingRunnable) {
    // ignoreExceptions() so that transient errors thrown while the Compose
    // accessibility tree catches up (e.g. NPEs from `find(...)!!` page-object
    // lookups) are treated as poll failures rather than fatally aborting the
    // wait — matching how AssertionError is already retried by untilAsserted.
    await().atMost(ofSeconds(seconds)).ignoreExceptions().untilAsserted(assertion)
}

fun memUsage(): String {
    System.gc()
    val free = getRuntime().freeMemory() / 1024 / 1024
    val total = getRuntime().totalMemory() / 1024 / 1024
    val max = getRuntime().maxMemory() / 1024 / 1024
    return "Free=$free, Total=$total, Max=$max MB"
}
