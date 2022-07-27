package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD1
internal class ConclusionTest {

    @Test
    fun construction() {
        val conclusion = Conclusion("Normal results.")
        assertEquals(conclusion.text, "Normal results.")
    }

    @Test
    fun jsonSerialisation() {
        val conclusion = Conclusion("Normal results.")
        val sd = serializeDeserialize(conclusion)
        assertEquals(sd, conclusion)
    }

    private fun serializeDeserialize(conclusion: Conclusion): Conclusion {
        val serialized = Json.encodeToString(conclusion)
        return Json.decodeFromString(serialized)
    }
}