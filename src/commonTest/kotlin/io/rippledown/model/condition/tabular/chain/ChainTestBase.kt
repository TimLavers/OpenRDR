package io.rippledown.model.condition.tabular.chain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class ChainTestBase {

    val t = listOf(true)
    val f = listOf(false)
    val tt = listOf(true, true)
    val tf = listOf(true, false)
    val ft = listOf(false, true)
    val ff = listOf(false, false)

    fun serializeDeserialize(chainPredicate: ChainPredicate): ChainPredicate {
        val serialized = Json.encodeToString(chainPredicate)
        return Json.decodeFromString(serialized)
    }
}