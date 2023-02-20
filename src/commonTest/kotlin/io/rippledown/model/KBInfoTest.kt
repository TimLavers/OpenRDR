package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KBInfoTest {

    @Test
    fun construction() {
        val info = KBInfo("Thyroids")
        assertEquals(info.name, "Thyroids")
    }

    @Test
    fun jsonSerialisation() {
        val info = KBInfo("Thyroids")
        val sd = serializeDeserialize(info)
        assertEquals(sd, info)
    }

    private fun serializeDeserialize(info: KBInfo): KBInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}