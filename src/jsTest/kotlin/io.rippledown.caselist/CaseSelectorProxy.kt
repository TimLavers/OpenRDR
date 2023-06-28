package io.rippledown.caselist

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.caseview.CASE_SELECTOR_ID
import proxy.findAllById
import proxy.findById
import proxy.findByTextContent
import proxy.requireElementContainingTextContent
import react.dom.test.Simulate
import react.dom.test.act
import web.html.HTMLElement

suspend fun HTMLElement.selectCaseById(caseId: Long) {
    val element = findById("$CASE_ID_PREFIX$caseId")
    act {
        Simulate.click(element)
    }
}

suspend fun HTMLElement.selectCaseByName(caseName: String) {
    val container = findById(CASE_SELECTOR_ID)
    val element = container.findByTextContent(caseName)
    act {
        Simulate.click(element)
    }
}

fun HTMLElement.requireCaseSelectorToBeShowing() {
    findById(CASE_SELECTOR_ID) shouldNotBe null
}

fun HTMLElement.requireCaseSelectorNotToBeShowing() {
    findAllById(CASE_SELECTOR_ID).length shouldBe 0
}

fun HTMLElement.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    val container = findById(CASE_SELECTOR_ID)
    caseNames.forEach { caseName ->
        container.requireElementContainingTextContent(caseName)
    }
}

fun HTMLElement.requireNameOnCaseListToBeSelected(caseName: String) {
    val container = findById(CASE_SELECTOR_ID)
    container.findByTextContent(caseName).classList.value shouldContain ("Mui-selected")
}