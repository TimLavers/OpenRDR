package io.rippledown

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    allowStructuredMapKeys = true
    ignoreUnknownKeys = true
    allowTrailingComma = true
    prettyPrint = true
    encodeDefaults = true
}

inline fun <reified T> T.toJsonString(): String {
    return json.encodeToString(this)
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> String.fromJsonString(): T {
    return json.decodeFromString<T>(this)
}

fun String.stripEnclosingJson() =
    replace("```json", "")
        .replace("```", "")
        .trim()

/**
 * Extracts individual JSON objects from a response string that may contain multiple JSON fragments.
 * Handles cases where the model returns multiple JSON objects separated by whitespace.
 *
 * A brace only opens a fragment when what follows it can begin a JSON object, i.e. a quoted member
 * name or the closing brace. Prose the model writes about comment variables, such as
 * "an attribute name in braces, e.g. {TSH}", is therefore not mistaken for an action.
 */
fun extractJsonFragments(response: String): List<String> {
    val fragments = mutableListOf<String>()
    val trimmed = response.trim()

    if (trimmed.isEmpty()) {
        return fragments
    }

    var braceCount = 0
    var start = -1

    for ((index, char) in trimmed.withIndex()) {
        if (char == '{') {
            if (braceCount == 0) {
                if (!opensJsonObject(trimmed, index)) continue
                start = index
            }
            braceCount++
        } else if (char == '}' && braceCount > 0) {
            braceCount--
            if (braceCount == 0) {
                fragments.add(trimmed.substring(start, index + 1))
                start = -1
            }
        }
    }

    return fragments
}

private fun opensJsonObject(text: String, braceIndex: Int): Boolean {
    val next = (braceIndex + 1..text.lastIndex).firstOrNull { !text[it].isWhitespace() } ?: return false
    return text[next] == '"' || text[next] == '}'
}

