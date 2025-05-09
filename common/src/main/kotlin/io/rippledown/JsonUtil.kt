package io.rippledown

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> T.toJsonString(): String {
    val json = Json {
        allowStructuredMapKeys = true
        prettyPrint = true
    }
    return json.encodeToString(this)
}