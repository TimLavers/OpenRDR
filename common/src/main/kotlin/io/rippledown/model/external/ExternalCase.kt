package io.rippledown.model.external

import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.TestEvent
import io.rippledown.model.TestResult
import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json

@Serializable
data class MeasurementEvent(val testName: String, val testTime: Long)

@Serializable
data class ExternalCase(val name: String, val data: Map<MeasurementEvent, TestResult>) {
    init {
        require(name.isNotBlank()) { "Name should not be blank." }
        require(data.isNotEmpty()) { "Data map should not be empty." }
    }
}

val jsonPretty = Json {
    prettyPrint = true
    allowStructuredMapKeys = true
}

fun ExternalCase.serialize() = jsonPretty.encodeToString(this)