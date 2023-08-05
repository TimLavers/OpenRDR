package io.rippledown.integration

import io.rippledown.constants.interpretation.DEBOUNCE_WAIT_PERIOD_MILLIS

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
