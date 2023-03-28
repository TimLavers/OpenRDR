package io.rippledown.model

fun daysAgo(n: Int): Long {
    return defaultDate - n * 24 * 60 * 60 * 1000
}

fun randomString(length: Int): String {
    //https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z')
    return List(length) { alphabet.random() }.joinToString("")
}

const val defaultDate = 1659752689505
const val today = defaultDate
val yesterday = daysAgo(1)
val  lastWeek = daysAgo(7)
