package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.rippledown.constants.interpretation.*
import proxy.findAllById
import proxy.findById
import web.html.HTMLElement

fun HTMLElement.requireConditions(expected: List<String>) {
    val found = findAllById(CONDITION_SELECTOR_ROW)
    found.length shouldBe expected.size
    expected.forEachIndexed { index, condition ->
        val row = found[index]
        row.textContent shouldBe condition
    }
}

fun HTMLElement.requireConditionsToBeSelected(expected: List<String>) {
    val allConditions = findAllById(CONDITION_SELECTOR_ROW)
    allConditions.forEach { element ->
        if (element.textContent in expected) {
            element.children[0].classList.value shouldContain "Mui-checked"
        }
    }
}

fun HTMLElement.requireConditionsToBeNotSelected(expected: List<String>) {
    val allConditions = findAllById(CONDITION_SELECTOR_CHECKBOX)
    allConditions.forEach { conditionElement ->
        if (conditionElement.textContent in expected)
            conditionElement.children[0].classList.value shouldNotContain "Mui-checked"
    }
}

fun HTMLElement.requireNumberOfConditions(count: Int) {
    findAllById(CONDITION_SELECTOR_ROW).length shouldBe count
}

fun HTMLElement.clickConditionWithIndex(index: Int) = findById("$CONDITION_SELECTOR_ROW$index").click()

fun HTMLElement.conditionSelectorButtons() = findById(CONDITION_SELECTOR_BUTTONS)
fun HTMLElement.clickDoneButton() = conditionSelectorButtons().findById(CONDITION_SELECTOR_DONE_BUTTON).click()
fun HTMLElement.requireDoneButtonShowing() =
    conditionSelectorButtons().findById(CONDITION_SELECTOR_DONE_BUTTON) shouldNotBe null

fun HTMLElement.requireCancelButtonShowing() =
    conditionSelectorButtons().findById(CONDITION_SELECTOR_CANCEL_BUTTON) shouldNotBe null

fun HTMLElement.requireDoneButtonNotShowing() = findAllById(CONDITION_SELECTOR_DONE_BUTTON).length shouldBe 0
fun HTMLElement.clickCancelButton() = findById(CONDITION_SELECTOR_CANCEL_BUTTON).click()


