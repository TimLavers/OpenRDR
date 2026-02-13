package io.rippledown.model.external

import io.rippledown.json
import io.rippledown.model.TestResult
import kotlinx.serialization.Serializable

@Serializable
data class MeasurementEvent(val testName: String, val testTime: Long)

@Serializable
data class ExternalCase(val name: String, val data: Map<MeasurementEvent, TestResult>) {
    init {
        require(name.isNotBlank()) { "Name should not be blank." }
        require(data.isNotEmpty()) { "Data map should not be empty." }
    }
}


fun ExternalCase.serialize() = json.encodeToString(this)