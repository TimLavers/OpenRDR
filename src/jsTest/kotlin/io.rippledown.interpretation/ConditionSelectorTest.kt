package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.condition.IsLow
import io.rippledown.model.condition.IsNormal
import kotlinx.coroutines.test.runTest
import react.FC
import react.dom.checkContainer
import kotlin.test.Test

class ConditionSelectorTest {
    private val attribute = Attribute(1, "x")
    private val isHigh = IsHigh(0, attribute)
    private val isLow = IsLow(1, attribute)
    private val isNormal = IsNormal(2, attribute)
    private val threeConditions = listOf(isHigh, isLow, isNormal)

    @Test
    fun shouldListConditionsThatAreHinted() = runTest {
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                requireConditions(threeConditions.map { it.asText() })
            }
        }
    }

    @Test
    fun shouldBeAbleToIdentifyTheSelectConditionsWhenDoneIsClicked() = runTest {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
                changedConditions = { _ ->
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickDoneButton()
                conditionsThatWereSelected shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun shouldBeAbleToIdentifyDeselectedConditionsWhenDoneIsClicked() = runTest {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
                changedConditions = { _ ->
                }
            }
        }
        checkContainer(fc) { container ->
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
    fun shouldBeAbleToSelectAndDeselectACondition() = runTest {
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { _ ->
                }
                changedConditions = { _ ->
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                requireConditionsToBeSelected(listOf())

                clickConditionWithIndex(0)
                requireConditionsToBeSelected(listOf(isHigh.asText()))

                clickConditionWithIndex(2)
                requireConditionsToBeSelected(listOf(isHigh.asText(), isNormal.asText()))

                clickConditionWithIndex(2) //de-select
                requireConditionsToBeSelected(listOf(isHigh.asText()))

                clickConditionWithIndex(0) //de-select
                requireConditionsToBeSelected(listOf())
            }
        }
    }


    @Test
    fun shouldBeAbleToCancelTheRule() = runTest {
        var cancelClicked = false
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onCancel = {
                    cancelClicked = true
                }
                changedConditions = { _ ->
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickCancelButton()
                cancelClicked shouldBe true
            }
        }
    }

    @Test
    fun updateShouldBeCalledWhenAConditionIsSelected() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                changedConditions = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                conditionsThatWereSelected shouldBe listOf(isHigh)
            }
        }
    }

    @Test
    fun updateShouldBeCalledWhenSeveralConditionsAreSelected() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                changedConditions = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                conditionsThatWereSelected shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun updateShouldBeCalledWhenAConditionIsDeselected() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                this.conditions = threeConditions
                changedConditions = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(0) //deselect
                conditionsThatWereSelected shouldBe emptyList()
            }
        }
    }

}