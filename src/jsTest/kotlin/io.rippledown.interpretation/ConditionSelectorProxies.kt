package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.constants.interpretation.CONDITION_SELECTOR_CANCEL_BUTTON
import io.rippledown.constants.interpretation.CONDITION_SELECTOR_DONE_BUTTON
import io.rippledown.constants.interpretation.CONDITION_SELECTOR_ROW
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

fun HTMLElement.clickConditionWithIndex(index: Int) = findById("$CONDITION_SELECTOR_ROW$index").click()

fun HTMLElement.clickDoneButton() = findById(CONDITION_SELECTOR_DONE_BUTTON).click()
fun HTMLElement.clickCancelButton() = findById(CONDITION_SELECTOR_CANCEL_BUTTON).click()


