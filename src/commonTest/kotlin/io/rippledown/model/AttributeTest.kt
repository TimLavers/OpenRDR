package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD1
internal class AttributeTest {

    @Test
    fun construction() {
        val tsh = Attribute("TSH")
        assertEquals(tsh.name, "TSH")
    }

    @Test
    fun jsonSerialisation() {
        val tsh = Attribute("TSH")
        val sd = serializeDeserialize(tsh)
        assertEquals(sd, tsh)
    }

    private fun serializeDeserialize(attribute: Attribute): Attribute {
        val serialized = Json.encodeToString(attribute)
        return Json.decodeFromString(serialized)
    }
}