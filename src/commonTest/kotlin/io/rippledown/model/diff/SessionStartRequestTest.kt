package io.rippledown.model.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.SessionStartRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class SessionStartRequestTest {
    @Test
    fun checkSerialization() {

        val sessionStartRequest = SessionStartRequest(
            caseId = 1,
            diff = Addition("Bring your handboard.")
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(sessionStartRequest)
        val deserialized = json.decodeFromString<SessionStartRequest>(serialized)
        deserialized shouldBe sessionStartRequest
    }
}