package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.condition.IsLow
import io.rippledown.model.condition.IsNormal
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import kotlin.test.Test

class ConditionSelectorTest {

    @Test
    fun shouldListConditionsThatAreHinted() = runTest {
        val attribute = Attribute("x")
        val conditions = listOf(IsHigh(attribute), IsLow(attribute), IsNormal(attribute))
        val vfc = VFC {
            ConditionSelector {
                conditionHints = conditions
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireConditions(conditions.map { it.asText() })
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectConditions() = runTest {
        val attribute = Attribute("x")
        val conditions = listOf(IsHigh(attribute), IsLow(attribute), IsNormal(attribute))
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val vfc = VFC {
            ConditionSelector {
                conditionHints = conditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickDoneButton()
                conditionsThatWereSelected shouldBe listOf(IsHigh(attribute), IsNormal(attribute))
            }
        }
    }

    @Test
    fun shouldBeAbleToDeselectACondition() = runTest {
        val attribute = Attribute("x")
        val conditions = listOf(IsHigh(attribute), IsLow(attribute), IsNormal(attribute))
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val vfc = VFC {
            ConditionSelector {
                conditionHints = conditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }

            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickConditionWithIndex(0) //de-select
                clickDoneButton()
                conditionsThatWereSelected shouldBe listOf(IsNormal(attribute))
            }
        }
    }

    @Test
    fun shouldBeAbleToCancelTheRule() = runTest {
        var cancelClicked = false
        val attribute = Attribute("x")
        val conditions = listOf(IsHigh(attribute), IsLow(attribute), IsNormal(attribute))
        val vfc = VFC {
            ConditionSelector {
                conditionHints = conditions
                onCancel = {
                    cancelClicked = true
                }

            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickCancelButton()
                cancelClicked shouldBe true
            }
        }
    }

}