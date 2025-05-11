package io.rippledown

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
