package io.rippledown.model.diff

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
        val diffList = DiffList(
            diffs = listOf(
                Unchanged("Go to Bondi Beach."),
                Addition("Bring your handboard."),
                Removal("Don't forget your towel."),
                Replacement("And have fun.", "And have lots of fun.")
            ),
            selected = 3
        )
        val conditionList = ConditionList(
            listOf(
                hasCurrentValue(1, Attribute(1, "a")),
                hasNoCurrentValue(2, Attribute(2, "b")),
                isLow(3, Attribute(3, "c"))
            )
        )
        val ruleRequest = RuleRequest(
            caseId = 1,
            diffList = diffList,
            conditionList = conditionList
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(ruleRequest)
        val deserialized = json.decodeFromString<RuleRequest>(serialized)
        deserialized shouldBe ruleRequest
    }
}