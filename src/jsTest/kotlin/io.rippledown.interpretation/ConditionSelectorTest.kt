package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import kotlinx.coroutines.test.runTest
import react.FC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class ConditionSelectorTest {
    private val attribute = Attribute(1, "x")
    private val isHigh = isHigh(0, attribute)
    private val isLow = isLow(1, attribute)
    private val isNormal = isNormal(2, attribute)
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
                conditionSelected = { _ ->
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
    fun shouldBeAbleToIdentifyADeselectedConditionWhenDoneIsClicked() = runTest {
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
        checkContainer(fc) { container ->
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
    fun shouldBeAbleToIdentifyDeselectedConditionsWhenDoneIsClicked() = runTest {
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
                conditionSelected = { _ ->
                }
            }
        }
        with(createRootFor(fc)) {
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

    @Test
    fun shouldBeAbleToCancel() = runTest {
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
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickCancelButton()
                cancelClicked shouldBe true
            }
        }
    }

    @Test
    fun shouldCallOnDoneWithNoConditions() = runTest {
        var onDoneCalled = false
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                onDone = {
                    onDoneCalled = true
                }
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                clickDoneButton()
                onDoneCalled shouldBe true
            }
        }
    }

    @Test
    fun shouldCallOnDoneWithTheSelectedConditions() = runTest {
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
        checkContainer(fc) { container ->
            with(container) {
                clickConditionWithIndex(0)
                clickConditionWithIndex(2)
                clickDoneButton()
                selectedConditionsWhenDone shouldBe listOf(isHigh, isNormal)
            }
        }
    }

    @Test
    fun conditionSelectedShouldBeCalledWhenAConditionIsSelected() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                conditionSelected = { selectedConditions ->
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
    fun conditionSelectedShouldIdentifyAllSelectedConditions() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                conditions = threeConditions
                conditionSelected = { selectedConditions ->
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
    fun conditionSelectedShouldBeCalledWhenAConditionIsDeselected() = runTest {
        var conditionsThatWereSelected = listOf<Condition>()
        val fc = FC {
            ConditionSelector {
                this.conditions = threeConditions
                conditionSelected = { selectedConditions ->
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