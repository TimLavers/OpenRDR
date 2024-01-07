package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.interpretation.CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.EMPTY_CORNERSTONE_VIEW_CONTAINER

// ORD2
class CornerstoneCaseViewPO() {

//    private fun cornerstoneContainerElement() = driver.findElement(By.id(CORNERSTONE_VIEW_CONTAINER))

    fun requireMessageForNoCornerstones(expected: String) {
//        driver.findElement(By.id(EMPTY_CORNERSTONE_VIEW_CONTAINER)).text shouldBe expected
    }

    fun requireCornerstoneCase(expectedCaseName: String) {
//        cornerstoneContainerElement().findElement(By.id(CASEVIEW_CORNERSTONE_CASE_NAME_ID)).text shouldBe expectedCaseName
    }

    fun selectNextCornerstoneCase() {
//        cornerstoneContainerElement()
//            .findElement(By.cssSelector("[aria-label='Go to next page']")).click()
    }

    fun selectPreviousCornerstoneCase() {
//        cornerstoneContainerElement()
//            .findElement(By.cssSelector("[aria-label='Go to previous page']")).click()
    }

    fun requireNumberOfCornerstones(expectedNumberOfCornerstones: Int) {
//        val listElements = cornerstoneContainerElement().findElements(By.tagName("li"))

        //first and last list elements are the navigation buttons
//        listElements.size - 2 shouldBe expectedNumberOfCornerstones
    }
}