package io.rippledown

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

inline fun <reified T> T.toJsonString(): String {
    val json = Json {
        allowStructuredMapKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    return json.encodeToString(this)
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> String.fromJsonString(): T {
    val json = Json {
        allowTrailingComma = true
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
    }
    return json.decodeFromString<T>(this)
}

fun String.stripEnclosingJson() =
    replace("```json", "")
        .replace("```", "")
        .trim()

/**
 * Extracts individual JSON objects from a response string that may contain multiple JSON fragments.
 * Handles cases where the model returns multiple JSON objects separated by whitespace.
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
        when (char) {
            '{' -> {
                if (braceCount == 0) {
                    start = index
                }
                braceCount++
            }

            '}' -> {
                braceCount--
                if (braceCount == 0 && start != -1) {
                    fragments.add(trimmed.substring(start, index + 1))
                    start = -1
                }
            }
        }
    }

    return fragments
}

