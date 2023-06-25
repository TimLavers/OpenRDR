package io.rippledown.model

import io.rippledown.CaseTestUtils
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseTest {

    @Test
    fun readFromFile() {
        val caseString = CaseTestUtils.caseData("Case1")
        val format = Json { allowStructuredMapKeys = true }
        val deserialized = format.decodeFromString<ExternalCase>(caseString)
        assertEquals(2, deserialized.data.size)
        assertEquals(deserialized.name, "Case1")
        assertEquals(deserialized.data[MeasurementEvent( "TSH", defaultTestDate)]!!.value.text, "0.667")
        assertEquals(deserialized.data[MeasurementEvent("ABC", defaultTestDate)]!!.value.text, "6.7")
    }
}