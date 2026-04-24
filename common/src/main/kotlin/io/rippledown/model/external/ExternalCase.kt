package io.rippledown.model.external

import io.rippledown.json
import io.rippledown.model.Result
import kotlinx.serialization.Serializable

@Serializable
data class MeasurementEvent(val name: String, val time: Long)

@Serializable
data class ExternalCase(val caseName: String, val data: Map<MeasurementEvent, Result>) {
    init {
        require(caseName.isNotBlank()) { "Name should not be blank." }
        require(data.isNotEmpty()) { "Data map should not be empty." }
    }
}


fun ExternalCase.serialize() = json.encodeToString(this)