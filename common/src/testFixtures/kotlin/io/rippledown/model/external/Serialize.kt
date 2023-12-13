package io.rippledown.model.external

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val jsonPretty = Json {
    prettyPrint = true
    allowStructuredMapKeys = true
}

fun ExternalCase.serialize() = jsonPretty.encodeToString(this)