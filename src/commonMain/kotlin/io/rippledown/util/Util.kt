package io.rippledown.util

import kotlin.random.Random

// https://www.baeldung.com/kotlin/random-alphanumeric-string
private val charPool : List<Char> = ('a'..'z') + ('0'..'9')
fun randomString(length: Int): String {
    return (1..length)
        .map { i -> Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}