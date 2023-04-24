package io.rippledown.util

import io.rippledown.model.condition.Condition
import kotlin.random.Random

// https://www.baeldung.com/kotlin/random-alphanumeric-string
private val charPool : List<Char> = ('a'..'z') + ('0'..'9')
fun randomString(length: Int): String {
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

fun Set<Condition>.findSameAs(condition: Condition) = this.firstOrNull { condition.sameAs(it) }

fun Set<Condition>.containsSameAs(condition: Condition) = findSameAs(condition) != null

fun Set<Condition>.findAbsentUsingSameAs(conditions: Set<Condition>) = conditions.filter { !this.containsSameAs(it) }.toSet()

infix fun Set<Condition>.shouldContainSameAs(it: Condition) {
    val match = this.findSameAs(it)
    require(match != null) {
        "No condition same as $it found in $this."
    }
}

infix fun Set<Condition>.shouldBeEqualUsingSameAs(value: Set<Condition>) {
    require(this.size == value.size) {
        "Set had ${value.size} conditions, but ${this.size} were expected."
    }
    val absent = this.findAbsentUsingSameAs(value)
    require (absent.isEmpty()) {
        "No conditions the same as $absent found in $value."
    }
}