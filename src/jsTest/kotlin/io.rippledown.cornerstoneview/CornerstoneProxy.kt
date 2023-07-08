package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import proxy.findAllById
import proxy.findById
import react.dom.test.Simulate
import react.dom.test.act
import web.html.HTMLElement

fun HTMLElement.requireSelectedCornerstoneOneBasedIndex(expectedIndex: Int) {
    val selected = querySelectorAll(".MuiPaginationItem-root.Mui-selected")
    selected.length shouldBe 1
    val selectedIndex = selected[0].textContent?.toInt() ?: -1
    selectedIndex shouldBe expectedIndex
}


fun HTMLElement.requireCornerstoneCaseToBeShowing(caseName: String) {
    findById(CASEVIEW_CORNERSTONE_CASE_NAME_ID).textContent shouldBe caseName
}

fun HTMLElement.requireCornerstoneCaseNotToBeShowing() {
    findAllById(CASEVIEW_CORNERSTONE_CASE_NAME_ID).length shouldBe 0
}

suspend fun HTMLElement.selectNextCornerstone() {
    val nextButton = querySelector("button[aria-label='Go to next page']")
    act {
        Simulate.click(nextButton!!)
    }
}

suspend fun HTMLElement.selectPreviousCornerstone() {
    val previousButton = querySelector("button[aria-label='Go to previous page']")
    act {
        Simulate.click(previousButton!!)
    }
}