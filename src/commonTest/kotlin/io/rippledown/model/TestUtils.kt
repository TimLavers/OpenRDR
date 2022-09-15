package io.rippledown.model

fun daysAgo(n: Int): Long {
    return defaultDate - n * 24 * 60 * 60 * 1000
}

const val defaultDate = 1659752689505
const val today = defaultDate
val yesterday = daysAgo(1)
val  lastWeek = daysAgo(7)
