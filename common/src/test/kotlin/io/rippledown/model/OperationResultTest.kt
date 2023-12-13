package io.rippledown.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OperationResultTest {

    @Test
    fun construction() {
        val result = OperationResult("Success!")
        assertEquals(result.message, "Success!")
    }

    @Test
    fun jsonSerialisation() {
        val result = OperationResult("Success!")
        val sd = serializeDeserialize(result)
        assertEquals(sd, result)
    }

    private fun serializeDeserialize(result: OperationResult): OperationResult {
        val serialized = Json.encodeToString(result)
        return Json.decodeFromString(serialized)
    }
}