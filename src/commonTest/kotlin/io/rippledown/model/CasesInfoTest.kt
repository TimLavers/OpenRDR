package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CasesInfoTest {

    @Test
    fun count() {
        val info = CasesInfo(99, "whatever/blah")
        assertEquals(info.count, 99)
        assertEquals(info.resourcePath, "whatever/blah")
    }

    @Test
    fun jsonSerialisation() {
        val info = CasesInfo(999, "blah/blah/blah")
        val sd1 = serializeDeserialize(info)
        assertEquals(sd1, info)
    }

    private fun serializeDeserialize(info: CasesInfo): CasesInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}