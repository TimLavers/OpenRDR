package io.rippledown.integration

fun stop() {
    Thread.sleep(120_000_000)
}
fun pause()  = pause(1000)

fun pause(period: Long) {
    Thread.sleep(period)
}