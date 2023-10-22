package io.rippledown.model.condition.episodic.signature

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class ChainTestBase {

    val t = listOf(true)
    val f = listOf(false)
    val tt = listOf(true, true)
    val tf = listOf(true, false)
    val ft = listOf(false, true)
    val ff = listOf(false, false)

    fun serializeDeserialize(signature: Signature): Signature {
        val serialized = Json.encodeToString(signature)
        return Json.decodeFromString(serialized)
    }
}