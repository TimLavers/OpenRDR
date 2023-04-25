package io.rippledown.util

import io.rippledown.model.condition.Condition
import kotlin.random.Random
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

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

// https://xa1.at/copy-data-class-reflection/
fun copyByReflection(instance: Any, newValues: Map<String, Any?>): Any {
    val instanceKClass = instance::class
    require(instanceKClass.isData) { "instance must be data class" }

    val copyFunction = instanceKClass.functions.single { function -> function.name == "copy" }

    val valueArgs = copyFunction.parameters
        .filter { parameter -> parameter.kind == KParameter.Kind.VALUE }
        .mapNotNull { parameter ->
            newValues[parameter.name]?.let { value -> parameter to value }
         }

    return copyFunction.callBy(
        mapOf(copyFunction.instanceParameter!! to instance) + valueArgs
    ) ?: error("copy didn't return a new instance")
}