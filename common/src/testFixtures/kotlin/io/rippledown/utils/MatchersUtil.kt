package io.rippledown.utils

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.model.condition.Condition


infix fun String?.shouldContainIgnoringMultipleWhitespace(substr: String): String? {
    if (this == null) {
        return null
    }
    val thisTrimmed = replace("\\s+".toRegex(), " ").trim()
    val substrTrimmed = substr.replace("\\s+".toRegex(), " ").trim()
    if (thisTrimmed.contains(substrTrimmed)) {
        return this
    }
    throw AssertionError("Expected '$thisTrimmed' to contain '$substrTrimmed'")
}

infix fun String?.shouldContainAll(expected: List<String>): String? {
    if (this == null) {
        return null
    }
    expected.all { phrase ->
        this shouldContain phrase
        true
    }
    return this
}
infix fun Condition?.shouldBeSameAs(expected: Condition): Condition? {
    this shouldNotBe null
    this?.sameAs(expected) shouldBe true
    return this
}
