package io.rippledown.caselist

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import proxy.findById
import web.html.HTMLElement

fun HTMLElement.requireNamesToBeShowingOnCaseList(vararg caseNames: String) {
    caseNames.forEach {
        findById("$CASE_ID_PREFIX$it").textContent shouldBe it
    }
}

fun HTMLElement.requireNameOnCaseListToBeSelected(caseName: String) {
    val classList = findById("$CASE_ID_PREFIX$caseName").classList.value
    classList shouldContain ("Mui-selected")
}