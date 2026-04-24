package io.rippledown.model.external

import io.kotest.matchers.shouldBe
import io.rippledown.utils.defaultDate
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class MeasurementEventTest {

    @Test
    fun construction() {
        val event = MeasurementEvent("Glucose", defaultDate)
        event.name shouldBe "Glucose"
        event.time shouldBe defaultDate
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