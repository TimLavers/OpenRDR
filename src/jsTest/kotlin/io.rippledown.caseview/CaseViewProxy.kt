package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_SELECTOR_ID
import proxy.findById
import proxy.findByTextContent
import proxy.requireElementContainingTextContent
import web.html.HTMLElement

fun HTMLElement.requireCaseToBeShowing(caseName: String) {
    findById(CASEVIEW_CASE_NAME_ID).textContent shouldBe caseName
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



