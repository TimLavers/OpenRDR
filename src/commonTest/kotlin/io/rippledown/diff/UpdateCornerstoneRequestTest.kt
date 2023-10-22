package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UpdateCornerstoneRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class UpdateCornerstoneRequestTest {
    @Test
    fun checkSerialization() {

        val request = UpdateCornerstoneRequest(
            CornerstoneStatus(),
            conditionList = ConditionList(listOf())
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<UpdateCornerstoneRequest>(serialized)
        deserialized shouldBe request
    }
}