package io.rippledown.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.rule.RuleRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class RuleRequestTest {
    @Test
    fun checkSerialization() {
        val conditionList = ConditionList(
            listOf(
                hasCurrentValue(1, Attribute(1, "a")),
                hasNoCurrentValue(2, Attribute(2, "b")),
                isLow(3, Attribute(3, "c"))
            )
        )
        val ruleRequest = RuleRequest(
            caseId = 1,
            conditions = conditionList
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(ruleRequest)
        val deserialized = json.decodeFromString<RuleRequest>(serialized)
        deserialized shouldBe ruleRequest
    }
}