package io.rippledown.model.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.condition.HasNoCurrentValue
import io.rippledown.model.condition.IsLow
import io.rippledown.model.rule.RuleRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class RuleRequestTest {
    @Test
    fun checkSerialization() {
        val conditionList = ConditionList(
            listOf(
                HasCurrentValue(1, Attribute(1, "a")),
                HasNoCurrentValue(2, Attribute(2, "b")),
                IsLow(3, Attribute(3, "c"))
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