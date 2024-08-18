package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class RuleConditionListTest {

    @Test
    fun check_serialization() {
        val conditionList = RuleConditionList(
            listOf(
                hasCurrentValue(1,Attribute(1, "x")),
                isHigh(2, Attribute(2, "y"))
            )
        )
        serializeDeserialize(conditionList) shouldBe conditionList
    }
}