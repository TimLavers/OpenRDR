package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseNameTest {

    @Test
    fun construction() {
        val caseName = CaseName("Case 1")
        assertEquals(caseName.name, "Case 1")
    }

    @Test
    fun jsonSerialisation() {
        val caseName = CaseName("Case 1")
        val sd = serializeDeserialize(caseName)
        assertEquals(sd, caseName)
    }

    private fun serializeDeserialize(caseName: CaseName): CaseName {
        val serialized = Json.encodeToString(caseName)
        return Json.decodeFromString(serialized)
    }
}