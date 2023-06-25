package io.rippledown.model.external

import io.kotest.matchers.shouldBe
import io.rippledown.model.defaultTestDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class MeasurementEventTest {

    @Test
    fun construction() {
        val event = MeasurementEvent("Glucose", defaultTestDate)
        event.testName shouldBe "Glucose"
        event.testTime shouldBe defaultTestDate
     }

    @Test
    fun jsonSerialisation() {
        val event = MeasurementEvent("Glucose", defaultTestDate)
        serializeDeserialize(event) shouldBe event
    }

    private fun serializeDeserialize(event: MeasurementEvent): MeasurementEvent {
        val serialized = Json.encodeToString(event)
        return Json.decodeFromString(serialized)
    }
}