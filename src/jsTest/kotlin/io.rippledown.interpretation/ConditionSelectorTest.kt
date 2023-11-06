package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isLow
import io.rippledown.model.condition.isNormal
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class ConditionSelectorTest {
    private val attribute = Attribute(1, "x")
    private val isHigh = isHigh(0, attribute)
    private val isLow = isLow(1, attribute)
    private val isNormal = isNormal(2, attribute)
    private val threeConditions = listOf(isHigh, isLow, isNormal)

    @Test
    fun shouldListConditionsThatAreHinted(): TestResult {
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireConditions(threeConditions.map { it.asText() })
            }
        }
    }

    @Test
    fun shouldBeAbleToIdentifyTheSelectConditionsWhenDoneIsClicked(): TestResult {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickDoneButton()
                conditionsThatWereSelected shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun shouldBeAbleToIdentifyADeselectedConditionWhenDoneIsClicked(): TestResult {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                conditionsThatWereSelected shouldBe emptyList()
                clickConditionWithIndex(0)
                clickConditionWithIndex(0) //de-select
                clickDoneButton()
                conditionsThatWereSelected shouldBe emptyList()
            }
        }
    }

    @Test
    fun shouldBeAbleToIdentifyDeselectedConditionsWhenDoneIsClicked(): TestResult {
        val conditionsThatWereSelected = mutableListOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { selectedConditions ->
                    conditionsThatWereSelected.addAll(selectedConditions)
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
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
    fun shouldBeAbleToSelectAndDeselectACondition(): TestResult {
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = { _ ->
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
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
    fun shouldBeAbleToCancel(): TestResult {
        var cancelClicked = false
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onCancel = {
                    cancelClicked = true
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickCancelButton()
                cancelClicked shouldBe true
            }
        }
    }

    @Test
    fun shouldCallOnDoneWithNoConditions(): TestResult {
        var onDoneCalled = false
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = {
                    onDoneCalled = true
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickDoneButton()
                onDoneCalled shouldBe true
            }
        }
    }

    @Test
    fun shouldCallOnDoneWithTheSelectedConditions(): TestResult {
        var selectedConditionsWhenDone = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = {
                    selectedConditionsWhenDone = it
                }
                conditionSelected = { _ ->
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickDoneButton()
                selectedConditionsWhenDone shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun conditionSelectedShouldBeCalledWhenAConditionIsSelected(): TestResult {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                conditionSelected = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                conditionsThatWereSelected shouldBe listOf(isHigh)
            }
        }
    }

    @Test
    fun conditionSelectedShouldIdentifyAllSelectedConditions(): TestResult {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                conditionSelected = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                conditionsThatWereSelected shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun conditionSelectedShouldBeCalledWhenAConditionIsDeselected(): TestResult {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                this.conditions = threeConditions
                conditionSelected = { selectedConditions ->
                    conditionsThatWereSelected = selectedConditions
                }
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(0) //deselect
                conditionsThatWereSelected shouldBe emptyList()
            }
        }
    }
}