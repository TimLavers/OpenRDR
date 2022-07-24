package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InterpretationTest {
    private val caseId = CaseId("1234", "Case 1")

    @Test
    fun construction() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        assertEquals(interpretation.caseId, caseId)
        assertEquals(interpretation.text, "Whatever, blah.")
    }

    @Test
    fun jsonSerialisation() {
        val interpretation = Interpretation(caseId, "Whatever, blah.")
        val sd = serializeDeserialize(interpretation)
        assertEquals(sd, interpretation)
    }

    private fun serializeDeserialize(interpretation: Interpretation): Interpretation {
        val serialized = Json.encodeToString(interpretation)
        return Json.decodeFromString(serialized)
    }
}