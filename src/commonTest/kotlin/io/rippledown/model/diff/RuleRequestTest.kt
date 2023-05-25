package io.rippledown.model.diff

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.HasCurrentValue
import io.rippledown.model.condition.HasNoCurrentValue
import io.rippledown.model.condition.IsLow
import kotlinx.serialization.decodeFromString
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
                HasCurrentValue(Attribute("a")),
                HasNoCurrentValue(Attribute("b")),
                IsLow(Attribute("c"))
            )
        )
        val ruleRequest = RuleRequest(
            caseId = "caseId",
            diffList = diffList,
            conditionList = conditionList
        )
        val json = Json { allowStructuredMapKeys = true }
        val serialized = json.encodeToString(ruleRequest)
        val deserialized = json.decodeFromString<RuleRequest>(serialized)
        deserialized shouldBe ruleRequest
    }
}