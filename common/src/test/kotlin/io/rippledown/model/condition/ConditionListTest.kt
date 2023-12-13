package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ConditionListTest {

    @Test
    fun check_serialization() {
        val conditionList = ConditionList(
            listOf(
                hasCurrentValue(1,Attribute(1, "x")),
                isHigh(2, Attribute(2, "y"))
            )
        )
        serializeDeserialize(conditionList) shouldBe conditionList
    }

    private fun serializeDeserialize(conditionList: ConditionList): ConditionList {
        val format = Json {
            allowStructuredMapKeys = true
            prettyPrint = true
        }
        return format.decodeFromString(format.encodeToString<ConditionList>(conditionList))
    }

}