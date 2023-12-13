package io.rippledown.model.external

import io.kotest.matchers.shouldBe
import io.rippledown.model.defaultDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class MeasurementEventTest {

    @Test
    fun construction() {
        val event = MeasurementEvent("Glucose", defaultDate)
        event.testName shouldBe "Glucose"
        event.testTime shouldBe defaultDate
    }

    @Test
    fun jsonSerialisation() {
        val event = MeasurementEvent("Glucose", defaultDate)
        serializeDeserialize(event) shouldBe event
    }

    private fun serializeDeserialize(event: MeasurementEvent): MeasurementEvent {
        val serialized = Json.encodeToString(event)
        return Json.decodeFromString(serialized)
    }
}