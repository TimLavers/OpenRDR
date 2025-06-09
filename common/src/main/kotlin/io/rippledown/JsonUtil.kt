package io.rippledown

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> T.toJsonString(): String {
    val json = Json {
        allowStructuredMapKeys = true
        prettyPrint = true
    }
    return json.encodeToString(this)
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> String.fromJsonString(): T {
    val json = Json {
        allowTrailingComma = true
    }
    return json.decodeFromString<T>(this)
}

fun String.stripEnclosingJson() =
    replace("```json\n", "")
        .replace("\n```", "")
        .trim()
