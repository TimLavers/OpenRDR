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
    val attribute = Attribute("x", 1)
    val isHigh = IsHigh(0, attribute)
    val isLow = IsLow(1, attribute)
    val isNormal = IsNormal(2, attribute)
    val conditions = listOf(isHigh, isLow, isNormal)

    @Test
    fun shouldListConditionsThatAreHinted() = runTest {
        val vfc = VFC {
            ConditionSelector {
                this.conditions = conditions
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
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val vfc = VFC {
            ConditionSelector {
                this.conditions = conditions
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
                conditionsThatWereSelected shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun shouldBeAbleToDeselectACondition() = runTest {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val vfc = VFC {
            ConditionSelector {
                this.conditions = conditions
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
                conditionsThatWereSelected shouldBe listOf(isNormal)
            }
        }
    }

    @Test
    fun shouldBeAbleToCancelTheRule() = runTest {
        var cancelClicked = false
        val vfc = VFC {
            ConditionSelector {
                this.conditions = conditions
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