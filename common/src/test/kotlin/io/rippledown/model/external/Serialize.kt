package io.rippledown.model.external

import kotlinx.serialization.encodeToString

fun ExternalCase.serialize() = jsonPretty.encodeToString(this)