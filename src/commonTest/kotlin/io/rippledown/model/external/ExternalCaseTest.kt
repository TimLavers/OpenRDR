package io.rippledown.model.external

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.TestResult
import io.rippledown.model.defaultTestDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

private val jsonPretty = Json {
    prettyPrint = true
    allowStructuredMapKeys = true
}

fun ExternalCase.serialize() = jsonPretty.encodeToString(this)

internal class ExternalCaseTest {
    private val date1 = defaultTestDate
    private val date2 = date1 - 100_034
    private val eventG1 = MeasurementEvent("Glucose", date1)
    private val eventG2 = MeasurementEvent("Glucose", date2)
    private val tr1 = TestResult("5.0")
    private val tr2 = TestResult("5.1")

    @Test
    fun nameNotEmpty() {
        shouldThrow<IllegalArgumentException> {
            ExternalCase("", mapOf(eventG1 to tr1))
        }.message shouldBe "Name should not be blank."
    }

    @Test
    fun dataNotEmpty() {
        shouldThrow<IllegalArgumentException> {
            ExternalCase("Blah", mapOf())
        }.message shouldBe "Data map should not be empty."
    }

    @Test
    fun construction() {
        val data = mapOf(eventG1 to tr1, eventG2 to tr2)

        val case = ExternalCase("Stuff", data)
        case.name shouldBe "Stuff"
        case.data shouldBe data
    }

    @Test
    fun jsonSerialisation() {
        val case = ExternalCase("Stuff", mapOf(eventG1 to tr1, eventG2 to tr2))
        serializeDeserialize(case) shouldBe case
    }

    private fun serializeDeserialize(event: ExternalCase): ExternalCase {
        return jsonPretty.decodeFromString(event.serialize())
    }
}