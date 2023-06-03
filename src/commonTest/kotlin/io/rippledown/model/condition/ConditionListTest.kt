package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ConditionListTest {

    @Test
    fun check_serialization() {
        val conditionList = ConditionList(
            listOf(
                HasCurrentValue(1,Attribute("x", 1)),
                IsHigh(2, Attribute("y", 2))
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